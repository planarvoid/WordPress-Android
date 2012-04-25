package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.WavHeader;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

public class WaveHeaderTest {
    /** 44100 16bit signed, 1 channel, 00:00:05.64, 497708 bytes  */
    public static final String SHORT_TEST_WAV = "/com/soundcloud/android/service/upload/short_test.wav";
    /** 44100 16bit signed, 2 channels, 00:00:18.95, 3342684 bytes  */
    public static final String MED_TEST_WAV = "/com/soundcloud/android/service/upload/med_test.wav";

    @Test
    public void shouldReadWaveHeaderFromInputStream() throws Exception {
        File file = new File(getClass().getResource(SHORT_TEST_WAV).toURI());
        InputStream wav = getClass().getResourceAsStream(SHORT_TEST_WAV);
        expect(wav).not.toBeNull();

        WavHeader header = new WavHeader(wav);
        expect(header.getSampleRate()).toEqual(44100);
        expect(header.getNumChannels()).toEqual((short) 1);
        expect(header.getFormat()).toEqual((short)1);
        expect(header.getBitsPerSample()).toEqual((short)16);
        expect(header.getBytesPerSample()).toEqual(2);
        expect(header.getNumBytes()).toEqual((int) file.length() - WavHeader.LENGTH);
    }

    @Test
    public void shouldCalculateDurationMed() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MED_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getDuration()).toEqual(18949l);
    }

    @Test
    public void shouldCalculateDurationShort() throws Exception {
        InputStream wav = getClass().getResourceAsStream(SHORT_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getDuration()).toEqual(5642l);
    }

    @Test
    public void shouldReturnMatchingAudioConfig_short() throws Exception {
        InputStream wav = getClass().getResourceAsStream(SHORT_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_1);
    }

    @Test
    public void shouldReturnMatchingAudioConfig_med() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MED_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_2);
    }

    @Test
    public void shouldCalculateOffsetMedium() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MED_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);

        expect(header.offset(1000)).toEqual(176444l);
        expect(header.offset(-1000)).toEqual(44l);
        expect(header.offset(Integer.MAX_VALUE)).toEqual(3342684l);
    }

    @Test
    public void shouldCalculateOffsetShort() throws Exception {
        InputStream wav = getClass().getResourceAsStream(SHORT_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);

        expect(header.offset(1000)).toEqual(88244l);
        expect(header.offset(-1000)).toEqual(44l);
        expect(header.offset(Integer.MAX_VALUE)).toEqual(497708l);
    }
}
