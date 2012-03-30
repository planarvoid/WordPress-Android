package com.soundcloud.android.jni;

import com.soundcloud.android.record.WaveHeader;

import android.os.Environment;

import java.io.File;
import java.io.InputStream;

public class VorbisEncoderTest extends TestBase {
    public void testEncodeShortHighQuality() throws Exception {
        encodeWav("short_test.wav", 5548, 1.0f);
    }

    public void testEncodeShortLowQuality() throws Exception {
        encodeWav("short_test.wav", 5052, 0.1f);
    }

    public void testEncodeMedHighQuality() throws Exception {
        encodeWav("med_test.wav", 18865, 1.0f);
    }

    public void testEncodeMedLowQuality() throws Exception {
        encodeWav("med_test.wav", 18705, 0.4f);
    }

    private void encodeWav(String file, int expectedDuration, float quality) throws Exception {
        assertEquals("need writable external storage",
                Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);

        InputStream in = assets().open(file);
        assertNotNull(in);
        WaveHeader waveHeader = new WaveHeader(in, true);

        final String ogg = file.replace(".wav", ".ogg");
        File out = externalPath(ogg);

        final long start = System.currentTimeMillis();
        VorbisEncoder.encodeWav(in, out, quality);
        final long duration = System.currentTimeMillis() - start;
        log("encoded '%s' in quality %f in %d ms, factor %.2f", file, quality, duration,
                (double) duration / (double) waveHeader.getDuration());

        checkAudioFile(out, expectedDuration);
    }
}
