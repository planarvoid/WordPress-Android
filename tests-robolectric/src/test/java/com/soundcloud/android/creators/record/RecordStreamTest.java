package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.shadows.ShadowNativeAmplitudeAnalyzer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Random;

@RunWith(DefaultTestRunner.class)
public class RecordStreamTest {
    private ByteArrayAudioWriter writer;

    @Before public void before() {
        writer = new ByteArrayAudioWriter();
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldRecordAmplitudeData() throws Exception {
        RecordStream rs = new RecordStream(AudioConfig.DEFAULT);
        rs.write(randomData(100), 100);

        expect(rs.getPreRecordAmplitudeData().isEmpty()).toBeFalse();
        expect(rs.getAmplitudeData().isEmpty()).toBeTrue();
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldWriteDataToWriters() throws Exception {
        RecordStream rs = new RecordStream(AudioConfig.DEFAULT);
        rs.setWriter(writer);
        final ByteBuffer buffer = randomData(100);
        rs.write(buffer, 100);

        expect(writer.getBytes().length).toEqual(100);
        expect(rs.getAmplitudeData().isEmpty()).toBeFalse();
//        expect(rs.getLastAmplitude()).toBeGreaterThan(0f);
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldReset() throws Exception {
        RecordStream rs = new RecordStream(AudioConfig.DEFAULT);
        final ByteBuffer buffer = randomData(100);
        rs.write(buffer, 100);
        rs.setWriter(writer);
        rs.write(buffer, 100);

        rs.reset();

        expect(rs.getAmplitudeData().isEmpty()).toBeTrue();
        expect(rs.getPreRecordAmplitudeData().isEmpty()).toBeTrue();
        expect(rs.getLastAmplitude()).toEqual(0f);
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldWriteFadeOut() throws Exception {
        ShadowNativeAmplitudeAnalyzer.lastValue = 20;

        RecordStream rs = new RecordStream(AudioConfig.DEFAULT);
        final ByteBuffer buffer = randomData(100);
        rs.setWriter(writer);
        rs.write(buffer, 100);
        rs.finalizeStream(null);
        expect(writer.getBytes().length).toEqual(8920);
    }

    private static ByteBuffer randomData(int length) {
        Random r = new Random(System.currentTimeMillis());
        ByteBuffer b = ByteBuffer.allocate(length);
        for (int i=0;i<length;i++) {
            b.put((byte) r.nextInt());
        }
        return b;
    }
}
