package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackFilter;
import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.audio.filter.FadeFilter;
import com.soundcloud.android.tests.AudioTestCase;
import junit.framework.AssertionFailedError;

import android.os.Environment;
import android.os.Parcel;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.Suppress;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

@LargeTest
public class VorbisEncoderTest extends AudioTestCase {

    public void testEncodeShortHighQuality() throws Exception {
        encodeWav(SHORT_WAV, 5548, EncoderOptions.HI_Q);
    }

    public void testEncodeShortLowQuality() throws Exception {
        encodeWav(SHORT_WAV, 5052, EncoderOptions.LO_Q);
    }

    public void testEncodeMedHighQuality() throws Exception {
        encodeWav(MED_WAV, 18865, EncoderOptions.HI_Q);
    }

    public void testEncodeMedLowQuality() throws Exception {
        encodeWav(MED_WAV, 18705, EncoderOptions.MED_Q);
    }

    public void testPartialEncoding() throws Exception {
        EncoderOptions opts = new EncoderOptions(1f, 2000, 5500, null, null);
        encodeWav(MED_WAV, 3500, opts);
    }

    public void testEncodingWithFadeFilter() throws Exception {
        FadeFilter filter = new FadeFilter(AudioConfig.PCM16_44100_1);
        EncoderOptions opts = new EncoderOptions(1f, 0, -1, null, filter);
        encodeWav(SINE_WAV, 10000, opts);
    }

    public void testEncodingWithNullFilter() throws Exception {
        PlaybackFilter filter = new PlaybackFilter() {
            @Override
            public ByteBuffer apply(ByteBuffer buffer, long position, long length) {
                for (int i = 0; i< buffer.capacity(); i++) {
                    buffer.put(i, (byte) 0);
                }
                return buffer;
            }
            @Override public int describeContents() {
                return 0;
            }
            @Override public void writeToParcel(Parcel dest, int flags) {
            }
        };

        EncoderOptions opts = new EncoderOptions(1f, 0, -1, null, filter);
        encodeWav(SHORT_WAV, 5052, opts);
    }

    public void testRelease() throws Exception {
        VorbisEncoder enc = createEncoder(externalPath("test.ogg"), AudioConfig.PCM16_44100_1);
        assertEquals(0, enc.getState());
        enc.release();
        assertEquals(-1, enc.getState());
    }

    public void testEncodeAndStartNewStream() throws Exception {
        File wav = prepareAsset(SHORT_WAV);
        InputStream is = new FileInputStream(wav);
        WavHeader h = new WavHeader(is);

        final File out = externalPath("test_encode_and_start_new_stream.ogg");

        VorbisEncoder enc = createEncoder(out, h.getAudioConfig());

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

    private void encodeWav(String file, int expectedDuration, EncoderOptions options) throws Exception {
        assertEquals("need writable external storage",
                Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);

        InputStream in = testAssets().open(file);
        assertNotNull(in);
        WavHeader wavHeader = new WavHeader(in, true);

        final String ogg = file.replace(".wav", ".ogg");
        File out = externalPath(ogg);

        final long start = System.currentTimeMillis();
        VorbisEncoder.encodeWav(in, out, options);
        final long duration = System.currentTimeMillis() - start;
        log("encoded '%s' in quality %f in %d ms, factor %.2f", file, options.quality, duration,
                (double) duration / (double) wavHeader.getDuration());

        assertTrue(
            String.format("Encoder did not produce valid ogg file (check %s with oggz-validate)", out.getAbsolutePath())
            , VorbisEncoder.validate(out));

        checkAudioFile(out, expectedDuration);
    }


    public void testWrite() throws Exception {
        File out = externalPath("testWrite.ogg");
        VorbisEncoder enc = new VorbisEncoder(out, "w", 1l, 44100l, 0.5f);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        int ret = enc.write(buffer, 300);
        assertEquals(300, ret);
        enc.pause();
        enc.release();
    }

    private VorbisEncoder createEncoder(File path, AudioConfig config) throws EncoderException {
        return new VorbisEncoder(path, "w", config.channels, config.sampleRate, config.quality);
    }
}
