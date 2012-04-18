package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.io.File;
import java.io.InputStream;

public class WaveHeaderTest {
    public static final String SHORT_TEST_WAV = "/com/soundcloud/android/service/upload/short_test.wav";
    public static final String MED_TEST_WAV = "/com/soundcloud/android/service/upload/med_test.wav";

    @Test
    public void shouldReadWaveHeaderFromInputStream() throws Exception {
        File file = new File(getClass().getResource(SHORT_TEST_WAV).toURI());
        InputStream wav = getClass().getResourceAsStream(SHORT_TEST_WAV);
        expect(wav).not.toBeNull();

        WaveHeader header = new WaveHeader(wav);
        expect(header.getSampleRate()).toEqual(44100);
        expect(header.getNumChannels()).toEqual((short) 1);
        expect(header.getFormat()).toEqual((short)1);
        expect(header.getBitsPerSample()).toEqual((short)16);
        expect(header.getNumBytes()).toEqual((int) file.length() - WaveHeader.LENGTH);
    }

    @Test
    public void shouldCalculateDuration() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MED_TEST_WAV);
        expect(wav).not.toBeNull();
        WaveHeader header = new WaveHeader(wav);
        expect(header.getDuration()).toEqual(18949l);
    }
}
