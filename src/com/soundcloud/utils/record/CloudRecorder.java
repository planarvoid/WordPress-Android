
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
    /**
     * INITIALIZING : recorder is initializing; READY : recorder has been
     * initialized, recorder not yet started RECORDING : recording ERROR :
     * reconstruction needed STOPPED: reset needed
     */
    public enum State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    };

    public static final boolean RECORDING_UNCOMPRESSED = true;

    public static final boolean RECORDING_COMPRESSED = false;

    // The interval in which the recorded samples are output to the file
    // Used only in uncompressed mode
    private static final int TIMER_INTERVAL = 100;

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

    // Number of bytes written to file after header(only in uncompressed mode)
    // after stop() is called, this size is written to the header/data chunk in
    // the wave file
    private int payloadSize;

    private final float MAX_VALUE = 1.0f / Short.MAX_VALUE;

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
                payloadSize += buffer.length;

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
                e.printStackTrace();
                Log.e(CloudRecorder.class.getName(),
                        "Error occured in updateListener, recording is aborted : ");
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
                    Log.w(CloudRecorder.class.getName(), "Increasing buffer size to "
                            + Integer.toString(bufferSize));
                }

                aRecorder = new AudioRecord(aSource, sRate, nChannels + 1, aFormat, bufferSize);
                if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                    throw new Exception("AudioRecord initialization failed");
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
                Log.e(CloudRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(CloudRecorder.class.getName(),
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
                        // prevent unexpected behavior in
                        // case the file already existed
                        /*
                         * fWriter.writeBytes("RIFF"); fWriter.writeInt(0); //
                         * Final file size not known yet, write 0
                         * fWriter.writeBytes("WAVE");
                         * fWriter.writeBytes("fmt ");
                         * fWriter.writeInt(Integer.reverseBytes(16)); //
                         * Sub-chunk size, 16 for PCM
                         * fWriter.writeShort(Short.reverseBytes((short) 1)); //
                         * AudioFormat, 1 for PCM
                         * fWriter.writeShort(Short.reverseBytes(nChannels));//
                         * Number of channels, 1 for mono, 2 for stereo
                         * fWriter.writeInt(Integer.reverseBytes(sRate)); //
                         * Sample rate
                         * fWriter.writeInt(Integer.reverseBytes(sRate
                         * *bSamples*nChannels/8)); // Byte rate,
                         * SampleRate*NumberOfChannels*BitsPerSample/8
                         * fWriter.writeShort
                         * (Short.reverseBytes((short)(nChannels*bSamples/8)));
                         * // Block align, NumberOfChannels*BitsPerSample/8
                         * fWriter.writeShort(Short.reverseBytes(bSamples)); //
                         * Bits per sample fWriter.writeBytes("data");
                         * fWriter.writeInt(0); // Data chunk size not known
                         * yet, write 0
                         */
                        buffer = new byte[framePeriod * bSamples / 8 * nChannels];
                        state = State.READY;
                    } else {
                        Log.e(CloudRecorder.class.getName(),
                                "prepare() method called on uninitialized recorder");
                        state = State.ERROR;
                    }
                } else {
                    mRecorder.prepare();
                    state = State.READY;
                }
            } else {
                Log.e(CloudRecorder.class.getName(), "prepare() method called on illegal state");
                release();
                state = State.ERROR;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(CloudRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(CloudRecorder.class.getName(), "Unknown error occured in prepare()");
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
                    Log.e(CloudRecorder.class.getName(),
                            "I/O exception occured while closing output file");
                }
                (new File(fPath)).delete();
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
            Log.e(CloudRecorder.class.getName(), e.getMessage());
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
                payloadSize = 0;

                state = State.RECORDING;
                aRecorder.startRecording();
                aRecorder.read(buffer, 0, buffer.length);

            } else {
                state = State.RECORDING;
                mRecorder.start();
                queueNextRefresh(TIMER_INTERVAL);
            }

        } else {
            Log.e(CloudRecorder.class.getName(), "start() called on illegal state");
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
                    /*
                     * fWriter.seek(4); // Write size to RIFF header
                     * fWriter.writeInt(Integer.reverseBytes(36+payloadSize));
                     * fWriter.seek(40); // Write size to Subchunk2Size field
                     * fWriter.writeInt(Integer.reverseBytes(payloadSize));
                     */
                    fWriter.close();
                } catch (IOException e) {
                    Log.e(CloudRecorder.class.getName(),
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
            Log.e(CloudRecorder.class.getName(), "stop() called on illegal state");
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
