package com.soundcloud.android.creators.record;

import org.jetbrains.annotations.NotNull;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.EnumSet;

public enum AudioConfig {
    PCM16_44100_2(16, 44100, 2),
    PCM16_44100_1(16, 44100, 1),
    PCM16_16000_1(16, 16000, 1),
    PCM16_22050_1(16, 22050, 1),
    PCM16_8000_1(16, 8000, 1),
    PCM8_8000_1(8, 8000, 1),;

    public final int sampleRate;
    public final int channels;
    public final int bitsPerSample;
    public final int sampleSize;
    public final int bytesPerSecond;
    public final int source = MediaRecorder.AudioSource.DEFAULT;

    public static final AudioConfig DEFAULT = PCM16_44100_1;

    private static AudioConfig detected;

    AudioConfig(int bitsPerSample, int sampleRate, int channels) {
        if (bitsPerSample != 8 && bitsPerSample != 16) {
            throw new IllegalArgumentException("invalid bitsPerSample:" + bitsPerSample);
        }
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("invalid channels:" + channels);
        }

        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
        this.channels = channels;
        sampleSize = (bitsPerSample / 8) * channels;
        bytesPerSecond = sampleRate * sampleSize;
    }


    /**
     * @param in true for input, false for output
     * @return the AudioFormat for this configuration
     */
    public int getChannelConfig(boolean in) {
        switch (channels) {
            case 1:
                return in ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return in ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_OUT_STEREO;
            default:
                return in ? AudioFormat.CHANNEL_IN_DEFAULT : AudioFormat.CHANNEL_OUT_DEFAULT;
        }
    }

    public int getFormat() {
        return bitsPerSample == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
    }

    public int getPlaybackMinBufferSize() {
        return AudioTrack.getMinBufferSize(sampleRate, getChannelConfig(false), getFormat());
    }

    public int getRecordMinBufferSize() {
        return AudioRecord.getMinBufferSize(sampleRate, getChannelConfig(true), getFormat());
    }

    public ScAudioTrack createAudioTrack(int bufferSize) {
        return new ScAudioTrack(this, bufferSize);
    }

    private AudioRecord createAudioRecord(int bufferSize) {
        return new AudioRecord(source, sampleRate, getChannelConfig(true), getFormat(), bufferSize);
    }

    public
    @NotNull
    AudioRecord createAudioRecord() {
        AudioRecord record = null;
        for (final int factor : new int[]{64, 32, 16, 8, 4, 1}) {
            try {
                record = createAudioRecord(getRecordMinBufferSize() * factor);
                if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                    return record;
                } else {
                    Log.w(AudioConfig.class.getSimpleName(), "audiorecord " + record + " in state " + record.getState());
                }
            } catch (Exception e) {
                Log.w(AudioConfig.class.getSimpleName(), e);
            }
        }
        if (record != null) {
            return record;
        } else {
            throw new RuntimeException("Could not create AudioRecord");
        }
    }

    public WavHeader createHeader() {
        return new WavHeader(WavHeader.FORMAT_PCM, (short) channels, sampleRate, (short) bitsPerSample, 0);
    }

    public RemainingTimeCalculator createCalculator() {
        return new RemainingTimeCalculator(bytesPerSecond);
    }

    public long msToByte(long ms) {
        return msToByte(ms, sampleRate, sampleSize);
    }

    public long bytesToMs(long bytePos) {
        return (1000 * bytePos) / bytesPerSecond;
    }

    public long validBytePosition(long offset) {
        return offset - (offset % ((bitsPerSample / 8) * channels));
    }

    /**
     * @return true if the system can handle this audio configuration.
     */
    public boolean isValid() {
        boolean valid;
        try {
            valid = getPlaybackMinBufferSize() > 0;
            if (!valid) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        try {
            valid = getRecordMinBufferSize() > 0;
        } catch (Exception e) {
            return false;
        }
        return valid;
    }

    public static AudioConfig findMatching(int sampleRate, int channels) {
        for (AudioConfig cfg : values()) {
            if (cfg.sampleRate == sampleRate && channels == cfg.channels) {
                return cfg;
            }
        }
        return null;
    }

    public static long msToByte(long ms, int sampleRate, int sampleSize) {
        return (long) (ms * (sampleRate / 1000d) * sampleSize);
    }


    /**
     * Tries to detect a working audio configuration.
     *
     * @return a working audio config, or {@link #DEFAULT} if not found
     */
    public static synchronized AudioConfig detect() {
        if (detected == null) {
            for (AudioConfig cfg : EnumSet.of(PCM16_44100_1, PCM16_22050_1, PCM16_16000_1, PCM16_8000_1)) {
                if (cfg.isValid()) {
                    detected = cfg;
                    break;
                }
            }
            if (detected == null) {
                // this will likely fail later
                Log.w("AudioConfig", "unable to detect valid audio config for this device");
                detected = DEFAULT;
            }
        }
        return detected;
    }
}
