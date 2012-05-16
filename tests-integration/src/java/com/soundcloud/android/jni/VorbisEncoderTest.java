package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.WavHeader;

import android.os.Environment;

import java.io.File;
import java.io.InputStream;

public class VorbisEncoderTest extends AudioTest {
    public void testEncodeShortHighQuality() throws Exception {
        encodeWav("audio/short_test.wav", 5548, 1.0f);
    }

    public void testEncodeShortLowQuality() throws Exception {
        encodeWav("audio/short_test.wav", 5052, 0.1f);
    }

    public void testEncodeMedHighQuality() throws Exception {
        encodeWav("audio/med_test.wav", 18865, 1.0f);
    }

    public void testEncodeMedLowQuality() throws Exception {
        encodeWav("audio/med_test.wav", 18705, 0.4f);
    }

    private void encodeWav(String file, int expectedDuration, float quality) throws Exception {
        assertEquals("need writable external storage",
                Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);

        InputStream in = testAssets().open(file);
        assertNotNull(in);
        WavHeader wavHeader = new WavHeader(in, true);

        final String ogg = file.replace(".wav", ".ogg");
        File out = externalPath(ogg);

        final long start = System.currentTimeMillis();
        VorbisEncoder.encodeWav(in, out, -1, quality, null);
        final long duration = System.currentTimeMillis() - start;
        log("encoded '%s' in quality %f in %d ms, factor %.2f", file, quality, duration,
                (double) duration / (double) wavHeader.getDuration());

        checkAudioFile(out, expectedDuration);
    }

    public void testRelease() throws Exception {
        VorbisEncoder enc = new VorbisEncoder(externalPath("test.ogg"), "w", AudioConfig.PCM16_44100_1);
        assertEquals(0, enc.getState());
        enc.release();
        assertEquals(-1, enc.getState());
    }
}
