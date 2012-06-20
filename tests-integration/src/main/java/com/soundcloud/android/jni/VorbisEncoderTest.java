package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.tests.AudioTestCase;
import junit.framework.AssertionFailedError;

import android.os.Environment;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.Suppress;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@LargeTest
public class VorbisEncoderTest extends AudioTestCase {

    public void testEncodeShortHighQuality() throws Exception {
        encodeWav(SHORT_WAV, 5548, 1.0f);
    }

    public void testEncodeShortLowQuality() throws Exception {
        encodeWav(SHORT_WAV, 5052, 0.1f);
    }

    public void testEncodeMedHighQuality() throws Exception {
        encodeWav(MED_WAV, 18865, 1.0f);
    }

    public void testEncodeMedLowQuality() throws Exception {
        encodeWav(MED_WAV, 18705, 0.4f);
    }

    public void testRelease() throws Exception {
        VorbisEncoder enc = new VorbisEncoder(externalPath("test.ogg"), "w", AudioConfig.PCM16_44100_1);
        assertEquals(0, enc.getState());
        enc.release();
        assertEquals(-1, enc.getState());
    }

    public void testEncodeAndStartNewStream() throws Exception {
        File wav = prepareAsset(SHORT_WAV);
        InputStream is = new FileInputStream(wav);
        WavHeader h = new WavHeader(is);

        final File out = externalPath("test_encode_and_start_new_stream.ogg");

        VorbisEncoder enc = new VorbisEncoder(out, "w", h.getAudioConfig());

        enc.encodeStream(is);

        // pause encode
        enc.pause();

        // reposition encoder
        enc.startNewStream(1.5d);

        VorbisDecoder d = new VorbisDecoder(out);
        assertEquals(1.5d, d.getInfo().duration, 0.3);
        d.release();
        assertTrue(VorbisEncoder.validate(out));

        // and write file again
        is = new FileInputStream(wav);
        // skip header
        new WavHeader(is);

        enc.encodeStream(is);
        enc.release();

        log("written file to "+out.getAbsolutePath());
        assertTrue(VorbisEncoder.validate(out));

        VorbisDecoder dec = new VorbisDecoder(out);
        VorbisInfo info = dec.getInfo();
        dec.release();

        // 1.5 + length of SHORT_WAV (5.64 secs)
        assertEquals(1.5d + 5.64d, info.duration, 0.4 /* TODO: should be more precise */);
    }

    @Suppress
    public void testManyEncodeAndStartNewStream() throws Exception {
        int failures = 0;
        for (int i = 0; i< 20; i++) {
            try {
                testEncodeAndStartNewStream();
            } catch (AssertionFailedError e) {
                log("failed");
                failures++;
            }
        }
        assertEquals(0, failures);
    }

    public void testExtract() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        File extracted =  externalPath("extracted.ogg");
        File extracted_wav =  externalPath("extracted.wav");

        VorbisEncoder.extract(ogg, extracted, 4.1d, 8.5d);

        assertTrue(VorbisEncoder.validate(extracted));
        checkAudioFile(extracted, 4333);

        VorbisDecoder decoder = new VorbisDecoder(extracted);
        VorbisInfo info = decoder.getInfo();
        assertEquals("got: "+info, 4.433d, info.duration, 0.001);
        assertEquals("got: " + info, 195520, info.numSamples);

        decoder.decodeToFile(extracted_wav);
        decoder.release();
    }

    public void testValidate() throws Exception {
        assertFalse(VorbisEncoder.validate(new File("/does/not/exist")));
        assertFalse(VorbisEncoder.validate(prepareAsset(SHORT_WAV)));
        assertTrue(VorbisEncoder.validate(prepareAsset(MED_TEST_OGG)));
        assertTrue(VorbisEncoder.validate(prepareAsset(SHORT_TEST_OGG)));
        assertFalse(VorbisEncoder.validate(prepareAsset(SHORT_TEST_NO_EOS_OGG)));
        assertTrue(VorbisEncoder.validate(prepareAsset(CHAINED_OGG)));
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

        assertTrue(
            String.format("Encoder did not produce valid ogg file (check %s with oggz-validate)", out.getAbsolutePath())
            , VorbisEncoder.validate(out));

        checkAudioFile(out, expectedDuration);
    }
}
