
package com.soundcloud.utils.record;

import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.service.CloudCreateService;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CloudRecorder {
    static final String TAG = CloudRecorder.class.getSimpleName();

    public static class Profile {
        public static final int ENCODED_LOW   = 0;
        public static final int ENCODED_HIGH  = 1;
        public static final int RAW           = 2;

        // best available quality on device
        public static int best() {
            return Build.VERSION.SDK_INT >= 10 ? ENCODED_HIGH : RAW;
        }

        public static int low() {
            return ENCODED_LOW;
        }
    }

    /**
     * INITIALIZING : recorder is initializing; READY : recorder has been
     * initialized, recorder not yet started RECORDING : recording ERROR :
     * reconstruction needed STOPPED: reset needed
     */
    public enum State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    }

    public static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(32768.0);

    // The interval in which the recorded samples are output to the file
    // Used only in uncompressed mode
    public static final int TIMER_INTERVAL = 50;

    // Recorder used for uncompressed recording
    private AudioRecord mAudioRecord = null;

    // Recorder used for compressed recording
    private MediaRecorder mRecorder = null;

    // Output file path
    private String mFilepath = null;

    // Recorder state; see State
    private State mState;

    // File writer (only in uncompressed mode)
    private RandomAccessFile mWriter;

    // Number of channels, sample rate, sample size(size in bits), buffer size,
    // audio source, sample size(see AudioFormat)
    private short nChannels;

    private int mSampleRate;

    private short mSamples;

    private int mAudioProfile;

    // Number of frames written to file on each output(only in uncompressed
    // mode)
    private int framePeriod;

    // Buffer for output(only in uncompressed mode)
    private byte[] buffer;

    private CloudCreateService service;

    /**
     * Default constructor Instantiates a new recorder, in case of compressed
     * recording the parameters can be left as 0. In case of errors, no
     * exception is thrown, but the state is set to ERROR
     */
    public CloudRecorder(int profile, int audioSource) {
        mAudioProfile = profile;

        switch (profile) {
            case Profile.RAW: {
                nChannels = 1;
                mSamples = 16;
                mSampleRate = ScCreate.PCM_REC_SAMPLE_RATE;
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
                mAudioRecord.setRecordPositionUpdateListener(updateListener);
                mAudioRecord.setPositionNotificationPeriod(framePeriod);
                break;
            }

            case Profile.ENCODED_HIGH:
            case Profile.ENCODED_LOW:
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(audioSource);
                mRecorder.setAudioSamplingRate(ScCreate.PCM_REC_SAMPLE_RATE);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                if (profile == Profile.ENCODED_LOW) {
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setAudioEncodingBitRate(12200);
                } else {
                    // only supported on API level => 10
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mRecorder.setAudioEncodingBitRate(96000);
                }
                break;
        }

        mFilepath = null;
        mState = State.INITIALIZING;
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

    /*
     * Method used for recording.
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
        public void onPeriodicNotification(AudioRecord recorder) {

            if (mState != State.RECORDING) return;

            int shortValue;
            float maxAmplitude = 0;

            mAudioRecord.read(buffer, 0, buffer.length); // Fill buffer
            try {
                mWriter.write(buffer); // Write buffer to file

                for (int i = 0; i < buffer.length / 2; i++) {
                    shortValue = getShort(buffer[i * 2], buffer[i * 2 + 1]);
                    if (Math.abs(shortValue) > maxAmplitude) {
                        maxAmplitude = Math.abs(shortValue);
                    }
                }

                if (service != null) {
                    // hack for not having a proper median. using a square root normalizes
                    // the amplitude and makes a better looking wave representation
                    service.onRecordFrameUpdate(((float)Math.sqrt(maxAmplitude))/MAX_ADJUSTED_AMPLITUDE);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                stop();
            }
        }

        public void onMarkerReached(AudioRecord recorder) {
            // NOT USED
        }
    };

    public void setRecordService(CloudCreateService service) {
        this.service = service;
    }



    // Sets output file path, call directly after construction/reset.
    public void setOutputFile(String argPath) {
        try {
            if (mState == State.INITIALIZING) {
                mFilepath = argPath;
                if (mAudioProfile != Profile.RAW) {
                    mRecorder.setOutputFile(mFilepath);
                }
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

    /**
     * Prepares the recorder for recording, in case the recorder is not in the
     * INITIALIZING state and the file path was not set the recorder is set to
     * the ERROR state, which makes a reconstruction necessary. In case
     * uncompressed recording is toggled, the header of the wave file is
     * written. In case of an exception, the state is changed to ERROR
     */
    public void prepare() {
        try {
            if (mState == State.INITIALIZING) {
                if (mAudioProfile == Profile.RAW) {
                    if ((mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) & (mFilepath != null)) {
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

                        buffer = new byte[framePeriod * mSamples / 8 * nChannels];
                        mState = State.READY;
                    } else {
                        Log.e(TAG, "prepare() method called on uninitialized recorder");
                        mState = State.ERROR;
                    }
                } else {
                    mRecorder.prepare();
                    mState = State.READY;
                }
            } else {
                Log.e(TAG, "prepare() method called on illegal state");
                release();
                mState = State.ERROR;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            } else {
                Log.e(TAG, "Unknown error occured in prepare()");
            }
            mState = State.ERROR;
        }
    }

    /**
     * Releases the resources associated with this class, and removes the
     * unnecessary files, when necessary
     */
    public void release() {
        if (mState == State.RECORDING) {
            stop();
        } else {
            if (mState == State.READY && mAudioProfile == Profile.RAW) {
                try {
                    mWriter.close();
                } catch (IOException e) {
                    Log.e(TAG, "I/O exception occured while closing output file");
                }
                if ((new File(mFilepath)).delete()) {
                    Log.v(TAG, "deleted " + mFilepath);
                }
            }
        }


        if (mAudioProfile == Profile.RAW) {
            if (mAudioRecord != null) {
                mAudioRecord.release();
            }
        } else {
            if (mRecorder != null) {
                mRecorder.release();
            }
        }
    }

    /**
     * Starts the recording, and sets the state to RECORDING. Call after
     * prepare().
     */
    public void start() {
        if (mState == State.READY) {
            if (mAudioProfile == Profile.RAW) {

                mState = State.RECORDING;
                mAudioRecord.startRecording();
                mAudioRecord.read(buffer, 0, buffer.length);

            } else {
                mState = State.RECORDING;
                mRecorder.start();
                queueNextRefresh(TIMER_INTERVAL);
            }

        } else {
            Log.e(TAG, "start() called on illegal state");
            mState = State.ERROR;
        }
    }

    /**
     * Stops the recording, and sets the state to STOPPED. In case of further
     * usage, a reset is needed. Also finalizes the wave file in case of
     * uncompressed recording.
     */
    public void stop() {
        if (mState == State.RECORDING) {
            if (mAudioProfile == Profile.RAW) {
                mAudioRecord.stop();
                try {
                    long length = mWriter.length();
                    // fill in missing header bytes
                    mWriter.seek(4);
                    mWriter.writeInt(Integer.reverseBytes((int) (length - 8)));
                    mWriter.seek(40);
                    mWriter.writeInt(Integer.reverseBytes((int) (length - 44)));
                    mWriter.close();
                } catch (IOException e) {
                    Log.e(TAG, "I/O exception occured while closing output file");
                    mState = State.ERROR;
                }
            } else {
                mRecorder.stop();
                refreshHandler.obtainMessage(REFRESH);
                refreshHandler.removeMessages(REFRESH);
                mLastRefresh = 0;

            }
            mState = State.STOPPED;
        } else {
            Log.e(TAG, "stop() called on illegal state");
            mState = State.ERROR;
        }
    }

    /**
     * Refresh functions, used for compressed recording notifications
     */

    private static final int REFRESH = 1;

    private long mLastRefresh = 0;

    private int mLastMax = 0;

    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:

                    if (service != null) {
                        int mCurrentMax = mRecorder.getMaxAmplitude();

                        // max amplitude returns false 0's sometimes, so just
                        // use the last value. It is usually only for a frame
                        if (mCurrentMax == 0) {
                            mCurrentMax = mLastMax;
                        } else {
                            mLastMax = mCurrentMax;
                        }

                        // hack for not having a proper median. using a square
                        // root normalizes the amplitude and makes a better
                        // looking wave representation
                        service.onRecordFrameUpdate(((float)Math.sqrt(mCurrentMax))/MAX_ADJUSTED_AMPLITUDE);
                    }

                    long next = TIMER_INTERVAL;
                    if (mLastRefresh == 0) {
                        mLastRefresh = System.currentTimeMillis();
                    } else {
                        long newDelay = TIMER_INTERVAL + TIMER_INTERVAL
                                - (System.currentTimeMillis() - mLastRefresh);
                        mLastRefresh = System.currentTimeMillis();
                        next = newDelay;
                    }
                    queueNextRefresh(next);
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

    // Converts a byte[2] to a short, in LITTLE_ENDIAN format
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }
}
