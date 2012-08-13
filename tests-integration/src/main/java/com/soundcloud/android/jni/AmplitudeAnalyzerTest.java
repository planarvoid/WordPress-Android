package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.record.JavaAmplitudeAnalyzer;
import com.soundcloud.android.tests.AudioTestCase;
import com.soundcloud.android.utils.BufferUtils;

import java.nio.ByteBuffer;

public class AmplitudeAnalyzerTest extends AudioTestCase {
    final static int SIZE = 1024;
    final static AudioConfig CONFIG = AudioConfig.PCM16_44100_1;

    JavaAmplitudeAnalyzer ja;
    NativeAmplitudeAnalyzer na;
    ByteBuffer buffer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ja = new JavaAmplitudeAnalyzer(CONFIG);
        na = new NativeAmplitudeAnalyzer(CONFIG);
        buffer = BufferUtils.allocateAudioBuffer(SIZE);
    }

    public void testEmptyBuffer() {
        clearData(buffer);
        assertEquals(0.1f, na.frameAmplitude(buffer, SIZE));
        assertEquals(0.1f, na.frameAmplitude(buffer, SIZE));
    }

    public void testRandomData() {
        fill(buffer, SIZE);
        float fj = ja.frameAmplitude(buffer, SIZE);
        float fn = na.frameAmplitude(buffer, SIZE);
        assertTrue(fj > 0.1f);
        assertTrue(fn > 0.1f);
        assertEquals(fj, fn, .0000001);
    }

    public void testStereoData() {
        fill(buffer, SIZE);
        assertEquals(new NativeAmplitudeAnalyzer(AudioConfig.PCM16_44100_2).frameAmplitude(buffer, SIZE),
                     new JavaAmplitudeAnalyzer(AudioConfig.PCM16_44100_2).frameAmplitude(buffer, SIZE),
                    .0000001);
    }


    private static void fill(ByteBuffer buffer, int size) {
        for (int i = 0; i < size / 2; i++) {
            buffer.putShort((short) (Math.random() * 32768));
        }
        buffer.rewind();
    }

    private static void clearData(ByteBuffer buffer) {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i, (byte) 0);
        }
    }
}
