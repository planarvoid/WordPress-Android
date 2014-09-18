package com.soundcloud.android.creators.record.jni;

import com.soundcloud.android.creators.record.AmplitudeAnalyzer;
import com.soundcloud.android.creators.record.AudioConfig;

import java.nio.ByteBuffer;

@SuppressWarnings("UnusedDeclaration")
public class NativeAmplitudeAnalyzer implements AmplitudeAnalyzer {
    private int last_max, last_value;
    private int current_adjusted_max_amplitude;

    @SuppressWarnings("FieldCanBeLocal")
    private final int channels, bytes_per_sample;

    public NativeAmplitudeAnalyzer(AudioConfig config) {
        channels = config.channels;
        bytes_per_sample = config.bitsPerSample / 8;
    }

    public native float frameAmplitude(ByteBuffer buffer, int length);

    public native int getLastValue();

    static {
        try {
            System.loadLibrary("native_amplitude_analyzer");
        } catch (UnsatisfiedLinkError e) {
            // only ignore exception in non-android env
            if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
                throw e;
            }
        }
    }
}
