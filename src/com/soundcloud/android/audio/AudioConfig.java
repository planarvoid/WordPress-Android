package com.soundcloud.android.audio;

import com.soundcloud.android.record.RemainingTimeCalculator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public enum AudioConfig {
    PCM16_44100_2(16, 44100, 2, .5f),
    PCM16_44100_1(16, 44100, 1, .5f),
    ;

    public final int sampleRate;
    public final int channels;
    public final int bitsPerSample;
    public final int bytesPerSecond;
    public final float quality;
    public final int source = MediaRecorder.AudioSource.MIC;

    public static AudioConfig DEFAULT = PCM16_44100_1;

    private AudioConfig(int bitsPerSample, int sampleRate, int channels, float quality) {
        if (bitsPerSample != 8 && bitsPerSample != 16) throw new IllegalArgumentException("invalid bitsPerSample:"+bitsPerSample);
        if (channels < 1 || channels > 2) throw new IllegalArgumentException("invalid channels:"+channels);

        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.quality = quality;
        bytesPerSecond = sampleRate * (bitsPerSample / 8) * channels;
    }

    public int getChannelConfig() {
        switch (channels) {
            case 1:  return AudioFormat.CHANNEL_CONFIGURATION_MONO;
            case 2:  return AudioFormat.CHANNEL_CONFIGURATION_STEREO;
            default: return AudioFormat.CHANNEL_CONFIGURATION_DEFAULT;
        }
    }

    public int getFormat() {
        return bitsPerSample == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(sampleRate, getChannelConfig(), getFormat());
    }

    public ScAudioTrack createAudioTrack(int bufferSize) {
        return new ScAudioTrack(this, bufferSize);
    }

    public AudioRecord createAudioRecord(int bufferSize) {
        return new AudioRecord(source, sampleRate, getChannelConfig(), getFormat(), bufferSize);
    }

    public WaveHeader createHeader() {
        return new WaveHeader(WaveHeader.FORMAT_PCM, (short)channels, sampleRate, (short)bitsPerSample, 0);
    }

    public RemainingTimeCalculator createCalculator() {
        return new RemainingTimeCalculator(bytesPerSecond);
    }

    public long msToByte(long ms) {
        return ms * bytesPerSecond / 1000;
    }

    public long bytesToMs(long bytePos){
        return (1000*bytePos)/(bytesPerSecond);
    }

    public long validBytePosition(long offset) {
       return offset - (offset % ((bitsPerSample / 8) * channels));
    }
}
