package com.soundcloud.android.audio;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.record.RemainingTimeCalculator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public enum AudioConfig {
    PCM16_44100_2(16, 44100, 2, .5f),
    PCM16_44100_1(16, 44100, 1, .5f),
    PCM16_8000_1 (16, 8000,  1, .5f),
    ;

    public final int sampleRate;
    public final int channels;
    public final int bitsPerSample;
    public final int sampleSize;
    public final int bytesPerSecond;
    public final float quality;
    public final int source = MediaRecorder.AudioSource.MIC;

    public static final AudioConfig DEFAULT = SoundCloudApplication.EMULATOR ?
            AudioConfig.PCM16_8000_1 : // also needs hw.audioInput=yes in avd
            AudioConfig.PCM16_44100_1;

    private AudioConfig(int bitsPerSample, int sampleRate, int channels, float quality) {
        if (bitsPerSample != 8 && bitsPerSample != 16) throw new IllegalArgumentException("invalid bitsPerSample:"+bitsPerSample);
        if (channels < 1 || channels > 2) throw new IllegalArgumentException("invalid channels:"+channels);

        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.quality = quality;
        sampleSize = (bitsPerSample / 8) * channels;
        bytesPerSecond = sampleRate * sampleSize;
    }


    /**
     * @param in true for input, false for output
     * @return the AudioFormat for this configuration
     */
    public int getChannelConfig(boolean in) {
        switch (channels) {
            case 1:  return in ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_OUT_MONO;
            case 2:  return in ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_OUT_STEREO;
            default: return in ? AudioFormat.CHANNEL_IN_DEFAULT : AudioFormat.CHANNEL_OUT_DEFAULT;
        }
    }

    public int getFormat() {
        return bitsPerSample == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(sampleRate, getChannelConfig(false), getFormat());
    }

    public ScAudioTrack createAudioTrack(int bufferSize) {
        return new ScAudioTrack(this, bufferSize);
    }

    public AudioRecord createAudioRecord(int bufferSize) {
        return new AudioRecord(source, sampleRate, getChannelConfig(true), getFormat(), bufferSize);
    }

    public WavHeader createHeader() {
        return new WavHeader(WavHeader.FORMAT_PCM, (short)channels, sampleRate, (short)bitsPerSample, 0);
    }

    public RemainingTimeCalculator createCalculator() {
        return new RemainingTimeCalculator(bytesPerSecond);
    }

    public long msToByte(long ms) {
        return (long) (ms * (sampleRate / 1000d) * (bitsPerSample / 8d) * channels);
    }

    public long bytesToMs(long bytePos){
        return (1000*bytePos) / bytesPerSecond;
    }

    public long validBytePosition(long offset) {
       return offset - (offset % ((bitsPerSample / 8) * channels));
    }

    public static AudioConfig findMatching(int sampleRate, int channels) {
        for (AudioConfig cfg : values()) {
            if (cfg.sampleRate == sampleRate && channels == cfg.channels) {
                return cfg;
            }
        }
        return null;
    }
}
