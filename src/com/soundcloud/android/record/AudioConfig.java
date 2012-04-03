package com.soundcloud.android.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

public enum AudioConfig {
    PCM16_44100_2(16, 44100, 2),
    PCM16_44100_1(16, 44100, 1),
    ;

    public final int sampleRate;
    public final int channels;
    public final int bitsPerSample;
    public final int bytesPerSecond;

    private AudioConfig(int bitsPerSample, int sampleRate, int channels) {
        if (bitsPerSample != 8 && bitsPerSample != 16) throw new IllegalArgumentException("invalid bitsPerSample:"+bitsPerSample);
        if (channels < 1 || channels > 2) throw new IllegalArgumentException("invalid channels:"+channels);

        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
        this.channels = channels;
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

    public AudioTrack createAudioTrack() {
        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, getChannelConfig(), getFormat(), getMinBufferSize(), AudioTrack.MODE_STREAM);
    }

    public AudioRecord createAudioRecord(int source, int bufferSize) {
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

    public long startPosition(long offset) {
       return offset - (offset % ((bitsPerSample / 8) * channels));
    }
}
