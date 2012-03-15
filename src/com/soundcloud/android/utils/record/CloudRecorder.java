
package com.soundcloud.android.utils.record;

import com.soundcloud.android.service.record.CloudCreateService;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import com.soundcloud.android.view.create.CreateController;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CloudRecorder {
    static final String TAG = CloudRecorder.class.getSimpleName();

    private State mState;
    public enum State {
        IDLE, READING, RECORDING, ERROR
    }

    private static final int REFRESH = 1;
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.log(32768.0) -4;
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

    private CloudCreateService service;
    private int mLastMax = 0;
    private int mCurrentAdjustedMaxAmplitude= 0;

    /**
     * Default constructor Instantiates a new recorder, in case of compressed
     * recording the parameters can be left as 0. In case of errors, no
     * exception is thrown, but the state is set to ERROR
     */
    public CloudRecorder(int audioSource) {
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
        mAudioRecord = new AudioRecord(audioSource, mSampleRate, nChannels + 1, audioFormat, bufferSize);
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
        if (mState == State.IDLE) {
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
            mState = State.READING;
        }

    }

    // Sets output file path, call directly after construction/reset.
    public void recordToFile(String argPath) {
        if (mState != State.READING) {
            startReading();
        }

        try {
            if (mState == State.READING) {
                mFilepath = argPath;
                // write file header
                mWriter = new RandomAccessFile(mFilepath, "rw");
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
                mState = State.RECORDING;
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
                long length = mWriter.length();
                // fill in missing header bytes
                mWriter.seek(4);
                mWriter.writeInt(Integer.reverseBytes((int) (length - 8)));
                mWriter.seek(40);
                mWriter.writeInt(Integer.reverseBytes((int) (length - 44)));
                mWriter.close();
                mWriter = null;
            } catch (IOException e) {
                Log.e(TAG, "I/O exception occured while closing output file");
                mState = State.ERROR;
            }
        }

        // stop reading as well, since we will go into playback mode now
        mAudioRecord.stop();
        mState = State.IDLE;
    }


    /**
     * Main loop of the audio reader.  This runs in its own thread.
     */
    private void readerRun() {
        while (mState == State.READING || mState == State.RECORDING) {
            long stime = System.currentTimeMillis();
            int shortValue;

            synchronized (buffer) {
                mAudioRecord.read(buffer, 0, buffer.length); // Fill buffer
                if (mWriter != null) {
                    try {
                        mWriter.write(buffer); // Write buffer to file
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
                } catch (InterruptedException e) {
                }
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
                            mCurrentAdjustedMaxAmplitude = (int) (mCurrentAdjustedMaxAmplitude * .8);
                        }

                        service.onRecordFrameUpdate((float) Math.max(.1f,
                                ((float) Math.log(mCurrentAdjustedMaxAmplitude) - 4)
                                / MAX_ADJUSTED_AMPLITUDE), mState == State.RECORDING);
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
    }

    // Converts a byte[2] to a short, in LITTLE_ENDIAN format
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }
}
