
package com.soundcloud.android.utils.record;

import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.view.create.CreateController;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class CloudRecorder {
    static final String TAG = CloudRecorder.class.getSimpleName();

     private static CloudRecorder instance = null;

    private State mState;
    private boolean destroyed;

    public enum State {
        IDLE, READING, RECORDING, ERROR
    }

    private static final int REFRESH = 1;
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(Math.sqrt(32768.0));
    public static final int TIMER_INTERVAL = 20;

    private AudioRecord mAudioRecord = null;
    private RandomAccessFile mWriter;
    private Thread readerThread = null;
    private String mFilepath = null;

    private short nChannels;
    private int mSampleRate;
    private short mSamples;

    private float mCurrentMaxAmplitude = 0;
    private int framePeriod;

    private byte[] buffer;
    private long mElapsedTime;

    private CloudCreateService service;
    private int mLastMax = 0;
    private int mCurrentAdjustedMaxAmplitude= 0;

    private RecordListener mRecListener;

    public List<Float> amplitudes = new ArrayList<Float>();
    public int writeIndex;

    public static CloudRecorder getInstance() {
      if(instance == null || instance.destroyed) {
         instance = new CloudRecorder();
      }
      return instance;
   }

    protected CloudRecorder() {
        nChannels = 2;
        mSamples = 16;
        mSampleRate = CreateController.REC_SAMPLE_RATE;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        framePeriod = mSampleRate * TIMER_INTERVAL / 1000;
        int bufferSize = framePeriod * 2 * mSamples * nChannels / 8;

        int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, nChannels + 1, audioFormat);

        if (bufferSize < minBufferSize) {
            // Check to make sure buffer size is not smaller than the smallest allowed one
            bufferSize = minBufferSize;
            // Set frame period and timer interval accordingly
            framePeriod = bufferSize / (2 * mSamples * nChannels / 8);
            Log.w(TAG, "Increasing buffer size to " + bufferSize);
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, nChannels + 1, audioFormat, bufferSize);
        buffer = new byte[framePeriod * mSamples / 8 * nChannels];
        mFilepath = null;
        mState = State.IDLE;
    }



    /**
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed
     * object. Useful, as no exceptions are thrown.
     *
     * @return recorder state
     */
    public State getState() {
        return mState;
    }


    public void setRecordService(CloudCreateService service) {
        this.service = service;
    }

    /**
     * Starts reading microphone input, and sets the state to READING.
     */

    public void startReading() {
        amplitudes.clear();
        writeIndex = -1;
        startReadingInternal(false);
    }

    private void startReadingInternal(final boolean setToRecording) {

        // check to see if we are already reading
        final boolean startReading = (mState != State.READING && mState != State.RECORDING);
        // avoid a race condition
        mState = setToRecording ? State.RECORDING : State.READING;
        if (startReading) {
            mAudioRecord.startRecording();
            readerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    readerRun();
                }
            }, "Audio Reader");

            readerThread.setPriority(Thread.MAX_PRIORITY);
            readerThread.start();
            queueNextRefresh(TIMER_INTERVAL);
        }
    }

    // Sets output file path, call directly after construction/reset.
    public void recordToFile(String argPath) {
        try {
            if (mState != State.RECORDING){
                mFilepath = argPath;
                // write file header
                mWriter = new RandomAccessFile(mFilepath, "rw");
                if (mWriter.length() == 0){
                    // new file
                    mWriter.setLength(0); // truncate
                    mWriter.writeBytes("RIFF");
                    mWriter.writeInt(0); // 36+numBytes
                    mWriter.writeBytes("WAVE");
                    mWriter.writeBytes("fmt ");
                    mWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
                    mWriter.writeShort(Short.reverseBytes((short) 1)); //  AudioFormat, 1 for PCM
                    mWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
                    mWriter.writeInt(Integer.reverseBytes(mSampleRate)); //  Sample rate
                    mWriter.writeInt(Integer.reverseBytes(mSampleRate * mSamples * nChannels / 8)); // Bitrate
                    mWriter.writeShort(Short.reverseBytes((short) (nChannels * mSamples / 8))); // Block align
                    mWriter.writeShort(Short.reverseBytes(mSamples)); // Bits per sample
                    mWriter.writeBytes("data");
                    mWriter.writeInt(0); // Data chunk size not known yet
                } else {
                    mWriter.seek(mWriter.length());
                }
                startReadingInternal(true);
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            } else {
                Log.e(TAG, "Unknown error occured while setting output path");
            }
            mState = State.ERROR;
        }
    }

    public void stop() {
        refreshHandler.removeMessages(REFRESH);
        if (mState == State.RECORDING) {
            // stop the file writing
            try {
                if (mWriter != null){
                    long length = mWriter.length();
                    // fill in missing header bytes
                    mWriter.seek(4);
                    mWriter.writeInt(Integer.reverseBytes((int) (length - 8)));
                    mWriter.seek(40);
                    mWriter.writeInt(Integer.reverseBytes((int) (length - 44)));
                    mWriter.close();
                    mWriter = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "I/O exception occured while closing output file");
                mState = State.ERROR;
            }
        }

        // stop reading as well, since we will go into playback mode now
        // this takes ~300 ms, so thread it low priority
        Thread stopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.stop();
            }
        });
        stopThread.setPriority(Thread.MIN_PRIORITY);
        stopThread.start();

        mState = State.IDLE;
    }


    /**
     * Main loop of the audio reader.  This runs in its own thread.
     */
    private void readerRun() {
        State saved = mState;
        while (mState == State.READING || mState == State.RECORDING) {
            long stime = System.currentTimeMillis();
            int shortValue;

            synchronized (buffer) {
                int read = mAudioRecord.read(buffer, 0, buffer.length); // Fill buffer
                if (mWriter != null) {
                    try {
                        mWriter.write(buffer,0,read); // Write buffer to file
                    } catch (IOException e) {
                        Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                        stop();
                    }
                }

                for (int i = 0; i < buffer.length / 2; i++) {
                    shortValue = getShort(buffer[i * 2], buffer[i * 2 + 1]);
                    if (Math.abs(shortValue) > mCurrentMaxAmplitude) {
                        mCurrentMaxAmplitude = Math.abs(shortValue);
                    }
                }

                long etime = System.currentTimeMillis();
                long sleep = TIMER_INTERVAL - (etime - stime);
                if (sleep < 5) {
                    sleep = 5;
                }

                try {
                    buffer.wait(sleep);
                } catch (InterruptedException ignore) {}
            }
        }
    }

    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:

                    if (mState != State.RECORDING && mState != State.READING) {
                        return;
                    }

                    int mCurrentMax = (int) mCurrentMaxAmplitude;
                    mCurrentMaxAmplitude = 0;

                    if (service != null) {

                        // max amplitude returns false 0's sometimes, so just
                        // use the last value. It is usually only for a frame
                        if (mCurrentMax == 0) {
                            mCurrentMax = mLastMax;
                        } else {
                            mLastMax = mCurrentMax;
                        }

                        if ( mCurrentMax >= mCurrentAdjustedMaxAmplitude )
                        {
                            /* When we hit a peak, ride the peak to the top. */
                            mCurrentAdjustedMaxAmplitude = mCurrentMax;
                        }
                        else
                        {
                            /*  decay of output when signal is low. */
                            mCurrentAdjustedMaxAmplitude = (int) (mCurrentAdjustedMaxAmplitude * .4);
                        }


                        if (mState == State.READING || mState == State.RECORDING) {
                            try {

                                if (mState == State.RECORDING && writeIndex == -1) {
                                    writeIndex = amplitudes.size();
                                }

                                mElapsedTime = mState != State.RECORDING ? -1 : PcmUtils.byteToMs(mWriter.length());
                                final float frameAmplitude = Math.max(.1f,
                                        ((float) Math.sqrt(Math.sqrt(mCurrentAdjustedMaxAmplitude)))
                                                / MAX_ADJUSTED_AMPLITUDE);

                                amplitudes.add(frameAmplitude);
                                if (mRecListener != null) mRecListener.onFrameUpdate(frameAmplitude, mElapsedTime);
                                service.onRecordFrameUpdate(mElapsedTime);
                            } catch (IOException e) {
                                Log.e(TAG, "Error accessing writer on frame update", e);
                            }
                        }
                    }

                    queueNextRefresh(TIMER_INTERVAL);
                    break;

                default:
                    break;
            }
        }
    };

    private void queueNextRefresh(long delay) {
        Message msg = refreshHandler.obtainMessage(REFRESH);
        refreshHandler.removeMessages(REFRESH);
        refreshHandler.sendMessageDelayed(msg, delay);
    }

    public void onDestroy(){
        if (mState == State.RECORDING) {
            stop();
        }
        mAudioRecord.release();
        refreshHandler.removeMessages(REFRESH);
        destroyed = true;
    }

    // Converts a byte[2] to a short, in LITTLE_ENDIAN format
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }

    public RecordListener getRecordListener() {
        return mRecListener;
    }

    public void setRecordListener(RecordListener listener) {
        this.mRecListener = listener;
    }

    public static interface RecordListener {
        void onFrameUpdate(float maxAmplitude, long elapsed);
    }
}
