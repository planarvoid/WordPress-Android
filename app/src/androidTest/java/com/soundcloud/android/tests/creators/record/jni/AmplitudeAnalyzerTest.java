package com.soundcloud.android.tests.creators.record.jni;

import com.soundcloud.android.creators.record.AudioConfig;
import com.soundcloud.android.creators.record.jni.NativeAmplitudeAnalyzer;
import com.soundcloud.android.tests.AudioTest;
import com.soundcloud.android.framework.annotation.NonUiTest;
import com.soundcloud.android.utils.BufferUtils;

import java.nio.ByteBuffer;

@NonUiTest
public class AmplitudeAnalyzerTest extends AudioTest {
    final static int SIZE = 1024;
    final static AudioConfig CONFIG = AudioConfig.PCM16_44100_1;

    NativeAmplitudeAnalyzer na;
    ByteBuffer buffer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        na = new NativeAmplitudeAnalyzer(CONFIG);
        buffer = BufferUtils.allocateAudioBuffer(SIZE);
    }

    public void ignore_testEmptyBuffer() {
        clearData(buffer);
        assertEquals(0.1f, na.frameAmplitude(buffer, SIZE));
    }

    public void ignore_testRandomData() {
        fill(buffer, SIZE);
        float fn = na.frameAmplitude(buffer, SIZE);
        assertTrue(fn > 0.1f);
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
