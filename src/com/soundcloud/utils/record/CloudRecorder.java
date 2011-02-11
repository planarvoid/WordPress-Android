
package com.soundcloud.utils.record;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.soundcloud.android.service.CloudCreateService;

public class CloudRecorder {
    static final String TAG = CloudRecorder.class.getSimpleName();

    /**
     * INITIALIZING : recorder is initializing; READY : recorder has been
     * initialized, recorder not yet started RECORDING : recording ERROR :
     * reconstruction needed STOPPED: reset needed
     */
    public enum State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    }

    // The interval in which the recorded samples are output to the file
    // Used only in uncompressed mode
    public static final int TIMER_INTERVAL = 50;

    // Toggles uncompressed recording on/off; RECORDING_UNCOMPRESSED /
    // RECORDING_COMPRESSED
    private boolean rUncompressed;

    // Recorder used for uncompressed recording
    private AudioRecord aRecorder = null;

    // Recorder used for compressed recording
    private MediaRecorder mRecorder = null;

    // Stores current amplitude (only in uncompressed mode)
    private int cAmplitude = 0;

    // Output file path
    private String fPath = null;

    // Recorder state; see State
    private State state;

    // File writer (only in uncompressed mode)
    private RandomAccessFile fWriter;

    // Number of channels, sample rate, sample size(size in bits), buffer size,
    // audio source, sample size(see AudioFormat)
    private short nChannels;

    private int sRate;

    private short bSamples;

    private int bufferSize;

    private int aSource;

    private int aFormat;

    // Number of frames written to file on each output(only in uncompressed
    // mode)
    private int framePeriod;

    // Buffer for output(only in uncompressed mode)
    private byte[] buffer;

    private CloudCreateService service;

    /**
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed
     * object. Useful, as no exceptions are thrown.
     * 
     * @return recorder state
     */
    public State getState() {
        return state;
    }

    /*
     * Method used for recording.
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
        public void onPeriodicNotification(AudioRecord recorder) {

            if (state != State.RECORDING)
                return;

            cAmplitude = 0;
            int shortValue;
            float maxAmplitude = 0;

            aRecorder.read(buffer, 0, buffer.length); // Fill buffer
            try {
                fWriter.write(buffer); // Write buffer to file

                for (int i = 0; i < buffer.length / 2; i++) {
                    shortValue = getShort(buffer[i * 2], buffer[i * 2 + 1]);
                    if (Math.abs(shortValue) > maxAmplitude)
                        maxAmplitude = Math.abs(shortValue);

                }

                if (service != null) {

                    // hack for not having a proper median. using a square root
                    // normalizes
                    // the amplitude and makes a better looking wave
                    // representation
                    service.onRecordFrameUpdate(((float)Math.sqrt(maxAmplitude))/MAX_ADJUSTED_AMPLITUDE);
                    // service.onRecordFrameUpdate((maxAmplitude) / MAX_AMPLITUDE);
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

    /**
     * Default constructor Instantiates a new recorder, in case of compressed
     * recording the parameters can be left as 0. In case of errors, no
     * exception is thrown, but the state is set to ERROR
     */
    public CloudRecorder(boolean uncompressed, int audioSource, int sampleRate, int channelConfig,
            int audioFormat) {
        try {
            rUncompressed = uncompressed;
            if (rUncompressed) { // RECORDING_UNCOMPRESSED
                if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                    bSamples = 16;
                } else {
                    bSamples = 8;
                }

                if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) {
                    nChannels = 1;
                } else {
                    nChannels = 2;
                }

                aSource = audioSource;
                sRate = sampleRate;
                aFormat = audioFormat;

                framePeriod = sampleRate * TIMER_INTERVAL / 1000;
                bufferSize = framePeriod * 2 * bSamples * nChannels / 8;
                if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                        audioFormat)) { // Check to make sure buffer size is not
                    // smaller than the smallest allowed one
                    bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                            audioFormat);
                    // Set frame period and timer interval accordingly
                    framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
                    Log.w(TAG, "Increasing buffer size to "
                            + Integer.toString(bufferSize));
                }

                aRecorder = new AudioRecord(aSource, sRate, nChannels + 1, aFormat, bufferSize);
                //if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                  //  throw new Exception("AudioRecord initialization failed");
                aRecorder.setRecordPositionUpdateListener(updateListener);
                aRecorder.setPositionNotificationPeriod(framePeriod);
            } else { // RECORDING_COMPRESSED
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

            }
            cAmplitude = 0;
            fPath = null;
            state = State.INITIALIZING;
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(CloudRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(CloudRecorder.class.getName(),
                        "Unknown error occured while initializing recording");
            }
            state = State.ERROR;
        }
    }

    /**
     * Sets output file path, call directly after construction/reset.
     * 
     * @param output file path
     */
    public void setOutputFile(String argPath) {
        try {
            if (state == State.INITIALIZING) {
                fPath = argPath;
                if (!rUncompressed) {
                    mRecorder.setOutputFile(fPath);
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            } else {
                Log.e(TAG,
                        "Unknown error occured while setting output path");
            }
            state = State.ERROR;
        }
    }

    /**
     * Returns the largest amplitude sampled since the last call to this method.
     * 
     * @return returns the largest amplitude since the last call, or 0 when not
     *         in recording state.
     */
    public int getMaxAmplitude() {
        if (state == State.RECORDING) {
            if (rUncompressed) {
                int result = cAmplitude;
                cAmplitude = 0;
                return result;
            } else {
                try {
                    return mRecorder.getMaxAmplitude();
                } catch (IllegalStateException e) {
                    return 0;
                }
            }
        } else {
            return 0;
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
            if (state == State.INITIALIZING) {
                if (rUncompressed) {
                    if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (fPath != null)) {
                        // write file header

                        fWriter = new RandomAccessFile(fPath, "rw");

                        fWriter.setLength(0); // Set file length to 0, to
                        buffer = new byte[framePeriod * bSamples / 8 * nChannels];
                        state = State.READY;
                    } else {
                        Log.e(TAG,
                                "prepare() method called on uninitialized recorder");
                        state = State.ERROR;
                    }
                } else {
                    mRecorder.prepare();
                    state = State.READY;
                }
            } else {
                Log.e(TAG, "prepare() method called on illegal state");
                release();
                state = State.ERROR;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            } else {
                Log.e(TAG, "Unknown error occured in prepare()");
            }
            state = State.ERROR;
        }
    }

    /**
     * Releases the resources associated with this class, and removes the
     * unnecessary files, when necessary
     */
    public void release() {
        if (state == State.RECORDING) {
            stop();
        } else {
            if ((state == State.READY) & (rUncompressed)) {
                try {
                    fWriter.close(); // Remove prepared file
                } catch (IOException e) {
                    Log.e(TAG,
                            "I/O exception occured while closing output file");
                }
                if ((new File(fPath)).delete()) {
                    Log.v(TAG, "deleted " + fPath);
                }
            }
        }

        if (rUncompressed) {
            if (aRecorder != null) {
                aRecorder.release();
            }
        } else {
            if (mRecorder != null) {
                mRecorder.release();
            }
        }
    }

    /**
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped. In
     * case of exceptions the class is set to the ERROR state.
     */
    public void reset() {
        try {
            if (state != State.ERROR) {
                release();
                fPath = null; // Reset file path
                cAmplitude = 0; // Reset amplitude
                if (rUncompressed) {
                    aRecorder = new AudioRecord(aSource, sRate, nChannels + 1, aFormat, bufferSize);
                    if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                        throw new Exception("AudioRecord initialization failed");
                    aRecorder.setRecordPositionUpdateListener(updateListener);
                    aRecorder.setPositionNotificationPeriod(framePeriod);
                } else {
                    mRecorder = new MediaRecorder();
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                }
                state = State.INITIALIZING;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            state = State.ERROR;
        }
    }

    /**
     * Starts the recording, and sets the state to RECORDING. Call after
     * prepare().
     */
    public void start() {
        Log.i("RECORDER", "START CALLED " + state);
        if (state == State.READY) {
            if (rUncompressed) {

                state = State.RECORDING;
                aRecorder.startRecording();
                aRecorder.read(buffer, 0, buffer.length);

            } else {
                state = State.RECORDING;
                mRecorder.start();
                queueNextRefresh(TIMER_INTERVAL);
            }

        } else {
            Log.e(TAG, "start() called on illegal state");
            state = State.ERROR;
        }
    }

    /**
     * Stops the recording, and sets the state to STOPPED. In case of further
     * usage, a reset is needed. Also finalizes the wave file in case of
     * uncompressed recording.
     */
    public void stop() {
        if (state == State.RECORDING) {
            if (rUncompressed) {
                aRecorder.stop();

                try {
                    fWriter.close();
                } catch (IOException e) {
                    Log.e(TAG,
                            "I/O exception occured while closing output file");
                    state = State.ERROR;
                }
            } else {
                mRecorder.stop();
                Message msg = refreshHandler.obtainMessage(REFRESH);
                refreshHandler.removeMessages(REFRESH);
                mLastRefresh = 0;

            }
            state = State.STOPPED;
        } else {
            Log.e(TAG, "stop() called on illegal state");
            state = State.ERROR;
        }
    }

    /**
     * Refresh functions, used for compressed recording notifications
     */

    private static final int REFRESH = 1;

    private long mLastRefresh = 0;

    private int mLastMax = 0;

    private int mCurrentMax;

    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:

                    if (service != null) {
                        mCurrentMax = mRecorder.getMaxAmplitude();

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
                        //service.onRecordFrameUpdate((mCurrentMax) / MAX_AMPLITUDE);
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

    final float MAX_AMPLITUDE = (float) 32768.0;

    final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(32768.0);

    // ******************************************************************** //
    // Constants.
    // ******************************************************************** //

    // Maximum signal amplitude for 16-bit data.
    private static final float MAX_16_BIT = 32768;

    private void queueNextRefresh(long delay) {
        Message msg = refreshHandler.obtainMessage(REFRESH);
        refreshHandler.removeMessages(REFRESH);
        refreshHandler.sendMessageDelayed(msg, delay);
    }

    /*
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     */
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }

}
