package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.utils.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class WavHeaderTest {

    @Test
    public void shouldReadWaveHeaderFromInputStream() throws Exception {
        File file = TestFiles.MONO_TEST_WAV.asFile();
        InputStream wav = TestFiles.MONO_TEST_WAV.asStream();
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
        WavHeader header = new WavHeader(TestFiles.STEREO_TEST_WAV.asStream());
        expect(header.getDuration()).toEqual(267l);
    }

    @Test
    public void shouldCalculateDurationMono() throws Exception {
        WavHeader header = new WavHeader(TestFiles.MONO_TEST_WAV.asStream());
        expect(header.getDuration()).toEqual(5642l);
    }

    @Test
    public void shouldCalculateDuration8Khz() throws Exception {
        WavHeader header = new WavHeader(TestFiles.PCM16_8000_1_WAV.asStream());
        expect(header.getDuration()).toEqual(5550l);
    }

    @Test
    public void shouldReturnMatchingAudioConfig_mono() throws Exception {
        WavHeader header = new WavHeader(TestFiles.MONO_TEST_WAV.asStream());
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_1);
    }

    @Test
    public void shouldReturnMatchingAudioConfig_stereo() throws Exception {
        WavHeader header = new WavHeader(TestFiles.STEREO_TEST_WAV.asStream());
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_2);
    }

    @Test
    public void shouldReturnMatchingAudio_8000() throws Exception {
        WavHeader header = new WavHeader(TestFiles.PCM16_8000_1_WAV.asStream());
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_8000_1);
    }

    @Test
    public void shouldCalculateOffsetStereo() throws Exception {
        WavHeader header = new WavHeader(TestFiles.STEREO_TEST_WAV.asStream());

        expect(header.offset(100)).toEqual(17684l);
        expect(header.offset(-1000)).toEqual(44l);
        expect(header.offset(Integer.MAX_VALUE)).toEqual(47148l);
    }

    @Test
    public void shouldCalculateOffsetShort() throws Exception {
        WavHeader header = new WavHeader(TestFiles.MONO_TEST_WAV.asStream());

        expect(header.offset(1000)).toEqual(88244l);
        expect(header.offset(-1000)).toEqual(44l);
        expect(header.offset(Integer.MAX_VALUE)).toEqual(497708l);
    }

    @Test
    public void shouldGetAudioData() throws Exception {
        WavHeader header = new WavHeader(TestFiles.MONO_TEST_WAV.asStream());

        WavHeader.AudioData data = header.getAudioData(1000, 3200);
        expect(data.length).toEqual(194040l);
        File out = File.createTempFile("partial_data", "wav");
        IOUtils.copy(data.stream, out);
        expect(out.length()).toEqual(194040l);
    }

    @Test
    public void shouldFixWavHeader() throws Exception {
        File tmp = File.createTempFile("tmp", "wav");
        WavHeader.writeHeader(tmp, 1000);

        // append some bytes
        FileOutputStream fos = new FileOutputStream(tmp, true);
        fos.write(new byte[8192]);
        fos.close();

        WavHeader.fixLength(new RandomAccessFile(tmp, "rw"));

        WavHeader header = WavHeader.fromFile(tmp);
        expect(header.getNumBytes()).toEqual(8192);
    }
}
