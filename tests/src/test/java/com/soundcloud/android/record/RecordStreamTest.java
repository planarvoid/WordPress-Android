package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.shadows.ShadowNativeAmplitudeAnalyzer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

@RunWith(DefaultTestRunner.class)
public class RecordStreamTest {
    private ByteArrayAudioWriter writer;

    @Before public void before() {
        writer = new ByteArrayAudioWriter();
    }

    @Test
    public void shouldRecordAmplitudeData() throws Exception {
        RecordStream rs = new RecordStream(AudioConfig.DEFAULT);
        rs.write(randomData(100), 100);

        expect(rs.getPreRecordAmplitudeData().isEmpty()).toBeFalse();
        expect(rs.getAmplitudeData().isEmpty()).toBeTrue();
    }

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

    @Test
    public void shouldCreateAmplitudeFile() throws Exception {
        File wavFile = TestFiles.PCM16_8000_1_WAV.asFile();
        final RecordStream rs = new RecordStream(AudioConfig.DEFAULT, wavFile, null, null);
        rs.regenerateAmplitudeData(null, null);
        expect(rs.getAmplitudeData().isEmpty()).toBeFalse();
        expect(rs.getAmplitudeData().size()).toEqual(31);
    }

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
