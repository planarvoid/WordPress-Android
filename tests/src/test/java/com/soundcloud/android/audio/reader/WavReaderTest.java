package com.soundcloud.android.audio.reader;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.record.TestFiles;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

@RunWith(DefaultTestRunner.class)
public class WavReaderTest {
    WavReader reader;

    @Before
    public void before() throws IOException {
        reader = new WavReader(TestFiles.MONO_TEST_WAV.asFile());
    }

    @Test
    public void shouldGetDuration() throws Exception {
        expect(reader.getDuration()).toEqual(5642L);
    }

    @Test
    public void shouldGetAudioConfig() throws Exception {
        expect(reader.getConfig()).toBe(AudioConfig.PCM16_44100_1);
    }

    @Test
    public void shouldGetPosition() throws Exception {
        expect(reader.getPosition()).toEqual(0L);
    }

    @Test
    public void shouldSeek() throws Exception {
        reader.seek(500);
        expect(reader.getPosition()).toEqual(500L);
    }

    @Test(expected = IOException.class)
    public void shouldThrowWithInvalidFile() throws Exception {
        File empty = File.createTempFile("empty", "wav");
        new WavReader(empty);
    }

    @Test
    public void shouldReopenAndReportCorrectDuration() throws Exception {
        File tmp = File.createTempFile("tmp", "wav");
        IOUtils.copy(TestFiles.MONO_TEST_WAV.asFile(), tmp);

        WavReader r = new WavReader(tmp);
        expect(r.getDuration()).toEqual(5642L);

        FileOutputStream fos = new FileOutputStream(tmp, true);
        fos.write(new byte[8192]);
        fos.close();

        expect(WavHeader.fixLength(new RandomAccessFile(tmp, "rw"))).toBeTrue();
        r.reopen();
        expect(r.getDuration()).toBeGreaterThan(5642L);
    }
}
