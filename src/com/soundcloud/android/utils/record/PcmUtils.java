package com.soundcloud.android.utils.record;

public class PcmUtils {

    public static int HIGH_QUALITY_STEREO_BYTES_PER_SECOND = (44100*2*2); //ms, sample rate * 2 bytes per sample * 2 channels

    public static long byteToMs(long bytePos){
        return byteToMs(bytePos, HIGH_QUALITY_STEREO_BYTES_PER_SECOND);
    }

    public static long byteToMs(long bytePos, int sampleRate, int bytesPerSample, int numChannels){
        return byteToMs(bytePos,sampleRate*bytesPerSample*numChannels);
    }

    public static long byteToMs(long bytePos, int bytesPerSecond){
        return (1000*bytePos)/(bytesPerSecond);
    }

    public static long msToByte(long ms){
        return msToByte(ms, HIGH_QUALITY_STEREO_BYTES_PER_SECOND);
    }

    public static long msToByte(long ms, int sampleRate, int bytesPerSample, int numChannels){
        return msToByte(ms,sampleRate*bytesPerSample*numChannels);
    }

    public static long msToByte(long ms, int bytesPerSecond){
        return ms*bytesPerSecond/1000;
    }
}
