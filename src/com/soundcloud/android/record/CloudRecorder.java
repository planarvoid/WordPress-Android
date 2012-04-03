
package com.soundcloud.android.record;

import com.soundcloud.android.jni.VorbisEncoder;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class CloudRecorder {
    private static final String TAG = CloudRecorder.class.getSimpleName();
    public static AudioConfig DEFAULT_CONFIG = AudioConfig.PCM16_44100_1;
    public static float ENCODING_QUALITY = 0.5f;
    private static CloudRecorder instance;

    // XXX memory
    public List<Float> amplitudes = new ArrayList<Float>();
    public int writeIndex;


    private State mState;
    private boolean destroyed;

    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING
    }

    private static final int REFRESH = 1;
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(Math.sqrt(32768.0));
    public static final int TIMER_INTERVAL = 20;

    private AudioRecord mAudioRecord;
    private VorbisEncoder mEncoder;
    private File mFile;
    private RandomAccessFile mWriter;

    final private AudioConfig mConfig;
    final private ByteBuffer buffer;

    private int mLastMax;
    private int mCurrentMaxAmplitude;
    private int mCurrentAdjustedMaxAmplitude;

    private RecordListener mRecListener;

    public static CloudRecorder getInstance() {
        if (instance == null || instance.destroyed) {
            instance = new CloudRecorder(MediaRecorder.AudioSource.MIC, DEFAULT_CONFIG);
        }
        return instance;
    }

    private CloudRecorder(int audioSource, AudioConfig config) {
        final int bufferSize = config.getMinBufferSize() * 3;
        mAudioRecord = config.createAudioRecord(audioSource, bufferSize);
        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        mFile = null;
        mState = State.IDLE;
        mConfig = config;
    }

    public void startReading() {
        amplitudes.clear();
        writeIndex = -1;
        startReadingInternal(false);
    }

    // Sets output file path, call directly after construction/reset.
    public State startRecording(File path) {
        try {
            if (mState != State.RECORDING) {
                mFile = path;
                mWriter = new RandomAccessFile(mFile, "rw");
                mEncoder = new VorbisEncoder(new File(path.getParentFile(), path.getName().concat(".ogg")), mConfig, ENCODING_QUALITY       );

                if (mWriter.length() == 0) {
                    Log.d(TAG, "creating new WAV file");
                    // new file
                    mWriter.setLength(0); // truncate
                    WaveHeader wh = mConfig.createHeader();
                    wh.write(mWriter);
                } else {
                    Log.d(TAG, "appending to existing WAV file");
                    mWriter.seek(mWriter.length());
                }
                startReadingInternal(true);
            } else throw new IllegalStateException("cannot record to file, in state "+mState);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            mState = State.ERROR;
        }
        return mState;
    }

    public void stop() {
        refreshHandler.removeMessages(REFRESH);
        // reader thread should stop automatically
        mState = State.STOPPING;
    }

    public void onDestroy() {
        if (mState == State.RECORDING) {
            stop();
        }
        mAudioRecord.release();
        refreshHandler.removeMessages(REFRESH);
        destroyed = true;
    }

    public RecordListener getRecordListener() {
        return mRecListener;
    }

    public void setRecordListener(RecordListener listener) {
        this.mRecListener = listener;
    }

    private State startReadingInternal(final boolean setToRecording) {
        // check to see if we are already reading
        final boolean startReading = mState != State.READING && mState != State.RECORDING;
        mState = setToRecording ? State.RECORDING : State.READING;
        if (startReading) {
            new ReaderThread().start();
            queueNextRefresh(TIMER_INTERVAL);
        }
        return mState;
    }

    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    if (mState != State.RECORDING && mState != State.READING) return;

                    int mCurrentMax = mCurrentMaxAmplitude;
                    mCurrentMaxAmplitude = 0;

                    // max amplitude returns false 0's sometimes, so just
                    // use the last value. It is usually only for a frame
                    if (mCurrentMax == 0) {
                        mCurrentMax = mLastMax;
                    } else {
                        mLastMax = mCurrentMax;
                    }

                    // Simple peak follower, cf. http://www.musicdsp.org/showone.php?id=19
                    if ( mCurrentMax >= mCurrentAdjustedMaxAmplitude ) {
                        /* When we hit a peak, ride the peak to the top. */
                        mCurrentAdjustedMaxAmplitude = mCurrentMax;
                    } else {
                        /*  decay of output when signal is low. */
                        mCurrentAdjustedMaxAmplitude = (int) (mCurrentAdjustedMaxAmplitude * .4);
                    }

                    try {
                        if (mState == State.RECORDING && writeIndex == -1) {
                            writeIndex = amplitudes.size();
                        }
                        long elapsedTime = mState != State.RECORDING ? -1 : mConfig.bytesToMs(mWriter.length());
                        final float frameAmplitude = Math.max(.1f,
                                ((float) Math.sqrt(Math.sqrt(mCurrentAdjustedMaxAmplitude)))
                                        / MAX_ADJUSTED_AMPLITUDE);

                        amplitudes.add(frameAmplitude);

                        if (mRecListener != null) mRecListener.onFrameUpdate(frameAmplitude, elapsedTime);
                    } catch (IOException e) {
                        Log.e(TAG, "Error accessing writer on frame update", e);
                    }

                    queueNextRefresh(TIMER_INTERVAL);
                    break;
                default:
            }
        }
    };

    private void queueNextRefresh(long delay) {
        Message msg = refreshHandler.obtainMessage(REFRESH);
        refreshHandler.removeMessages(REFRESH);
        refreshHandler.sendMessageDelayed(msg, delay);
    }

    private class ReaderThread extends Thread {
        ReaderThread() {
            super("ReaderThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            Log.d(TAG, "starting reader thread");

            mAudioRecord.startRecording();
            while (mState == CloudRecorder.State.READING || mState == CloudRecorder.State.RECORDING) {
                final long start = System.currentTimeMillis();

                buffer.rewind();
                final int read = mAudioRecord.read(buffer, buffer.capacity());

                if (read < 0) {
                    Log.w(TAG, "AudioRecord.read() returned error: " + read);
                    mState = CloudRecorder.State.ERROR;
                } else if (read == 0) {
                    Log.w(TAG, "AudioRecord.read() returned no data");
                } else {
                    if (mWriter != null) {
                        try {
                            final int written = mWriter.getChannel().write(buffer);

                            if (written < read) {
                                Log.w(TAG, "partial write "+written);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                            mState = CloudRecorder.State.ERROR;
                            break;
                        }
                    }

                    if (mEncoder != null) {
                        mEncoder.addSamples(buffer, read);
                    }

                    buffer.rewind();
                    buffer.limit(read);
                    while (buffer.position() < buffer.limit()) {
                        int value;
                        switch (mConfig.bitsPerSample) {
                            case 16:
                                value = buffer.getShort();
                                break;
                            case 8:
                                value = buffer.get();
                                break;
                            default:
                                value = 0;
                        }
                        if (value > mCurrentMaxAmplitude) mCurrentMaxAmplitude = value;
                    }
                }
                SystemClock.sleep(Math.max(5,
                        TIMER_INTERVAL - (System.currentTimeMillis() - start)));
            }
            Log.d(TAG, "exiting reader loop, stopping recording (mState="+mState+")");

            finalizeFile();
            mAudioRecord.stop();
            mState = CloudRecorder.State.IDLE;
        }

        private void finalizeFile() {
            try {
                if (mWriter != null) {
                    final long length = mWriter.length();
                    Log.d(TAG, "finalising recording file (length="+length+")");
                    if (length == 0) {
                        Log.w(TAG, "file length is zero");
                    } else if (length > WaveHeader.LENGTH) {
                        // fill in missing header bytes
                        mWriter.seek(4);
                        mWriter.writeInt(Integer.reverseBytes((int) (length - 8)));
                        mWriter.seek(40);
                        mWriter.writeInt(Integer.reverseBytes((int) (length - 44)));
                    } else {
                        Log.w(TAG, "data length is zero");
                    }
                    mWriter.close();
                    mWriter = null;
                }

                if (mEncoder != null) {
                    mEncoder.finish();
                }
            } catch (IOException e) {
                Log.e(TAG, "I/O exception occured while finalizing file", e);
            }
        }
    }
}
