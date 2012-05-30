package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.WavHeader;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

public class WavHeaderTest {
    /** 44100 16bit signed, 1 channel, 00:00:05.64, 497708 bytes  */
    public static final String MONO_TEST_WAV = "/com/soundcloud/android/record/mono_16bit_44khz.wav";
    /** 44100 16bit signed, 2 channels, 00:00:00.27, 47148 bytes  */
    public static final String STEREO_TEST_WAV = "/com/soundcloud/android/record/stereo_16bit_44khz.wav";
    /** 8000 16bit signed, 1 channel, 00:00:05.55, 88844 byta es  */
    public static final String PCM16_8000_1_WAV = "/com/soundcloud/android/record/PCM16_8000_1.wav";

    @Test
    public void shouldReadWaveHeaderFromInputStream() throws Exception {
        File file = new File(getClass().getResource(MONO_TEST_WAV).toURI());
        InputStream wav = getClass().getResourceAsStream(MONO_TEST_WAV);
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
    public void shouldCalculateDurationStereo() throws Exception {
        InputStream wav = getClass().getResourceAsStream(STEREO_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getDuration()).toEqual(267l);
    }

    @Test
    public void shouldCalculateDurationMono() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MONO_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getDuration()).toEqual(5642l);
    }

    @Test
    public void shouldCalculateDuration8Khz() throws Exception {
        InputStream wav = getClass().getResourceAsStream(PCM16_8000_1_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getDuration()).toEqual(5550l);
    }

    @Test
    public void shouldReturnMatchingAudioConfig_mono() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MONO_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_1);
    }

    @Test
    public void shouldReturnMatchingAudioConfig_stereo() throws Exception {
        InputStream wav = getClass().getResourceAsStream(STEREO_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_2);
    }

    @Test
    public void shouldReturnMatchingAudio_8000() throws Exception {
        InputStream wav = getClass().getResourceAsStream(PCM16_8000_1_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_8000_1);
    }

    @Test
    public void shouldCalculateOffsetStereo() throws Exception {
        InputStream wav = getClass().getResourceAsStream(STEREO_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);

        expect(header.offset(100)).toEqual(17684l);
        expect(header.offset(-1000)).toEqual(44l);
        expect(header.offset(Integer.MAX_VALUE)).toEqual(47148l);
    }

    @Test
    public void shouldCalculateOffsetShort() throws Exception {
        InputStream wav = getClass().getResourceAsStream(MONO_TEST_WAV);
        expect(wav).not.toBeNull();
        WavHeader header = new WavHeader(wav);

        expect(header.offset(1000)).toEqual(88244l);
        expect(header.offset(-1000)).toEqual(44l);
        expect(header.offset(Integer.MAX_VALUE)).toEqual(497708l);
    }
}
