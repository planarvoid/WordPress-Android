
package com.soundcloud.android.utils.record;

import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.view.create.CreateController;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class CloudRecorder {
    static final String TAG = CloudRecorder.class.getSimpleName();

    private static CloudRecorder instance;

    private State mState;
    private boolean destroyed;

    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING
    }

    private static final int REFRESH = 1;
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(Math.sqrt(32768.0));
    public static final int TIMER_INTERVAL = 20;

    private AudioRecord mAudioRecord = null;
    private RandomAccessFile mWriter;
    private String mFilepath = null;

    private int nChannels;
    private int mSampleRate;
    private int mBitsPerSample;

    private int mCurrentMaxAmplitude = 0;

    private byte[] buffer;

    private CloudCreateService service;
    private int mLastMax = 0;
    private int mCurrentAdjustedMaxAmplitude= 0;

    private RecordListener mRecListener;

    // XXX memory
    public List<Float> amplitudes = new ArrayList<Float>();
    public int writeIndex;

    public static interface RecordListener {
        void onFrameUpdate(float maxAmplitude, long elapsed);
    }

    public static CloudRecorder getInstance() {
        if (instance == null || instance.destroyed) {
            instance = new CloudRecorder(MediaRecorder.AudioSource.MIC, CreateController.REC_SAMPLE_RATE, 2, 16);
        }
        return instance;
    }

    private CloudRecorder(int audioSource, int sampleRate, int channels, int bitsPerSample) {
        if (bitsPerSample != 8 && bitsPerSample != 16) throw new IllegalArgumentException("invalid bitsPerSample:"+bitsPerSample);
        if (channels < 1 || channels > 2) throw new IllegalArgumentException("invalid channels:"+channels);

        nChannels      = channels;
        mBitsPerSample = bitsPerSample;
        mSampleRate    = sampleRate;

        final int audioFormat = bitsPerSample == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        final int channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        final int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, channelConfig, audioFormat);

        int framePeriod = mSampleRate * TIMER_INTERVAL / 1000;
        int bufferSize = framePeriod * 2 * mBitsPerSample * nChannels / 8;
        if (bufferSize < minBufferSize) {
            // Check to make sure buffer size is not smaller than the smallest allowed one
            bufferSize = minBufferSize;
            // Set frame period and timer interval accordingly
            framePeriod = bufferSize / (2 * mBitsPerSample * nChannels / 8);
            Log.w(TAG, "Increasing buffer size to " + bufferSize);
        }
        mAudioRecord = new AudioRecord(audioSource, mSampleRate, channelConfig, audioFormat, bufferSize);
        buffer = new byte[framePeriod * mBitsPerSample / 8 * nChannels];
        mFilepath = null;
        mState = State.IDLE;
    }

    public void setRecordService(CloudCreateService service) {
        this.service = service;
    }

    public void startReading() {
        amplitudes.clear();
        writeIndex = -1;
        startReadingInternal(false);
    }

    // Sets output file path, call directly after construction/reset.
    public State startRecording(String path) {
        try {
            if (mState != State.RECORDING) {
                mFilepath = path;

                mWriter = new RandomAccessFile(mFilepath, "rw");
                if (mWriter.length() == 0) {
                    // new file
                    mWriter.setLength(0); // truncate
                    WaveHeader wh = new WaveHeader(WaveHeader.FORMAT_PCM, (short)nChannels, mSampleRate, (short)mBitsPerSample, 0);
                    wh.write(mWriter);
                } else {
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
                    if (service != null) {
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
                            long elapsedTime = mState != State.RECORDING ? -1 : PcmUtils.byteToMs(mWriter.length());
                            final float frameAmplitude = Math.max(.1f,
                                    ((float) Math.sqrt(Math.sqrt(mCurrentAdjustedMaxAmplitude)))
                                            / MAX_ADJUSTED_AMPLITUDE);

                            amplitudes.add(frameAmplitude);
                            if (mRecListener != null) mRecListener.onFrameUpdate(frameAmplitude, elapsedTime);

                            service.onRecordFrameUpdate(elapsedTime);
                        } catch (IOException e) {
                            Log.e(TAG, "Error accessing writer on frame update", e);
                        }
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
        public synchronized void run() {
            Log.d(TAG, "starting reader thread");

            mAudioRecord.startRecording();
            while (mState == CloudRecorder.State.READING || mState == CloudRecorder.State.RECORDING) {
                final long start = System.currentTimeMillis();
                final int read = mAudioRecord.read(buffer, 0, buffer.length);
                if (mWriter != null) {
                    try {
                        mWriter.write(buffer, 0, read);
                    } catch (IOException e) {
                        Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                        mState = CloudRecorder.State.ERROR;
                        break;
                    }
                }
                for (int i = 0; i < buffer.length / (mBitsPerSample/8); i++) {
                    int value;
                    switch (mBitsPerSample) {
                        case 16:
                            value = Math.abs((short) (buffer[i * 2] | (buffer[i * 2 + 1] << 8)));
                            break;
                        case 8:
                           value = Math.abs(buffer[i]);
                            break;
                        default:
                            value = 0;
                    }
                    if (value > mCurrentMaxAmplitude) mCurrentMaxAmplitude = value;
                }

                // ???
                try {
                    wait(Math.max(5, TIMER_INTERVAL - (System.currentTimeMillis() - start)));
                } catch (InterruptedException ignore) {}
            }
            Log.d(TAG, "exiting reader loop, stopping recording (mState="+mState+")");

            finalizeFile();
            mAudioRecord.stop();
            mState = CloudRecorder.State.IDLE;
        }

        private void finalizeFile() {
            try {
                if (mWriter != null) {
                    long length = mWriter.length();
                    Log.d(TAG, "finalising recording file (length="+length+")");
                    // fill in missing header bytes
                    mWriter.seek(4);
                    mWriter.writeInt(Integer.reverseBytes((int) (length - 8)));
                    mWriter.seek(40);
                    mWriter.writeInt(Integer.reverseBytes((int) (length - 44)));
                    mWriter.close();
                    mWriter = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "I/O exception occured while finalizing file", e);
            }
        }
    }
}
