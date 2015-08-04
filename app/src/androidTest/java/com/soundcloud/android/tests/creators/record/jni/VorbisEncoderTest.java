package com.soundcloud.android.tests.creators.record.jni;

import com.soundcloud.android.creators.record.AudioConfig;
import com.soundcloud.android.creators.record.PlaybackFilter;
import com.soundcloud.android.creators.record.WavHeader;
import com.soundcloud.android.creators.record.filter.FadeFilter;
import com.soundcloud.android.creators.record.jni.EncoderException;
import com.soundcloud.android.creators.record.jni.EncoderOptions;
import com.soundcloud.android.creators.record.jni.ProgressListener;
import com.soundcloud.android.creators.record.jni.VorbisDecoder;
import com.soundcloud.android.creators.record.jni.VorbisEncoder;
import com.soundcloud.android.creators.record.jni.VorbisInfo;
import com.soundcloud.android.creators.upload.UserCanceledException;
import com.soundcloud.android.tests.AudioTest;
import com.soundcloud.android.framework.annotation.NonUiTest;
import com.soundcloud.android.framework.annotation.SlowTest;
import com.soundcloud.android.utils.IOUtils;
import junit.framework.AssertionFailedError;

import android.os.Debug;
import android.os.Environment;
import android.os.Parcel;
import android.os.SystemClock;
import android.test.suitebuilder.annotation.Suppress;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

@SlowTest @NonUiTest
public class VorbisEncoderTest extends AudioTest {

    private static final boolean PROFILE = false;

    public void ignore_testEncodeShortHighQuality() throws Exception {
        encodeWav(SHORT_MONO_WAV, 5548, EncoderOptions.HI_Q);
    }

    public void ignore_testEncodeShortDefaultQuality() throws Exception {
        encodeWav(SHORT_MONO_WAV, 5052, EncoderOptions.DEFAULT);
    }

    public void ignore_testEncodeMedHighQuality() throws Exception {
        encodeWav(MED_STEREO_WAV, 18865, EncoderOptions.HI_Q);
    }

    public void ignore_testEncodeMedDefaultQuality() throws Exception {
        encodeWav(MED_STEREO_WAV, 18865, EncoderOptions.DEFAULT);
    }

    public void ignore_testEncodeMedLowQuality() throws Exception {
        encodeWav(MED_STEREO_WAV, 18705, EncoderOptions.LO_Q);
    }

    public void ignore_testEncodePartialWav() throws Exception {
        EncoderOptions opts = new EncoderOptions(1f, 2000, 5500, null, null);
        encodeWav(MED_STEREO_WAV, 3500, opts);
    }

    public void ignore_testEncodingWithFadeFilter() throws Exception {
        FadeFilter filter = new FadeFilter(AudioConfig.PCM16_44100_1);
        EncoderOptions opts = new EncoderOptions(.5f, 0, -1, null, filter);
        encodeWav(SINE_WAV, 10000, opts);
    }

    public void ignore_testEncodeVorbis() throws Exception {
        FadeFilter filter = new FadeFilter(AudioConfig.PCM16_44100_1);
        final long[] m = new long[1];
        final ProgressListener listener = new ProgressListener() {
            @Override public void onProgress(long current, long max) throws UserCanceledException {
                m[0] = max;
            }
        };

        EncoderOptions opts = new EncoderOptions(.5f, 0, -1, listener, filter);
        encodeVorbis(SHORT_TEST_OGG, 5.642d, opts);
        assertEquals(5, m[0]);
    }

    public void ignore_testEncodePartialVorbis() throws Exception {
        EncoderOptions opts = new EncoderOptions(.5f, 500, 1500, null, null);
        encodeVorbis(SHORT_TEST_OGG, 1d, opts);
    }

    public void ignore_testEncodeAndGuessFileType() throws Exception {
        final File ogg = prepareAsset(SHORT_TEST_OGG);
        final File renamed = new File(ogg.getParentFile(), String.valueOf(System.currentTimeMillis()));
        assertTrue(ogg.renameTo(renamed));
        renamed.deleteOnExit();
        final File ogg_output = externalPath(renamed + "-re-encoded.ogg");

        VorbisEncoder.encodeFile(renamed, ogg_output, EncoderOptions.DEFAULT);

        assertTrue(ogg_output.exists());
    }

    @Suppress
    public void ignore_testEncodeHugeWavFile() throws Exception {
        long space = IOUtils.getSpaceLeft(Environment.getExternalStorageDirectory());
        long encodedSpace = space / 10; // assume 1:10 compression ratio
        long length = space - encodedSpace - (1024 * 1024) /* headroom */;

        // around an hour worth of recording
        File wav = createWavFile((int) Math.min(length, 1024 * 1024 * 300));
        File out = externalPath("out.ogg");

        EncoderOptions opts = new EncoderOptions(1f, 0, -1, new ProgressListener() {
            @Override
            public void onProgress(long current, long max) throws UserCanceledException {
                int percent = (int) Math.min(100, Math.round(100 * (current / (double) max)));
                log("progress: %d / %d (%d%%)", current, max, percent);
            }
        }, null);
        log("encoding file of size "+wav.length());
        VorbisEncoder.encodeWav(wav, out, opts);
    }

    public void ignore_testEncodingWithNullFilter() throws Exception {
        PlaybackFilter filter = new PlaybackFilter() {
            @Override
            public ByteBuffer apply(ByteBuffer buffer, long position, long length) {
                for (int i = 0; i< buffer.remaining(); i++) {
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
        encodeWav(SHORT_MONO_WAV, 5052, opts);
    }

    public void ignore_testRelease() throws Exception {
        VorbisEncoder enc = createEncoder(externalPath("test.ogg"), AudioConfig.PCM16_44100_1);
        assertEquals(0, enc.getState());
        enc.release();
        assertEquals(-1, enc.getState());
    }

    public void ignore_testEncodeAndStartNewStream() throws Exception {
        File wav = prepareAsset(SHORT_MONO_WAV);
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
    public void ignore_testManyEncodeAndStartNewStream() throws Exception {
        int failures = 0;
        for (int i = 0; i< 20; i++) {
            try {
                ignore_testEncodeAndStartNewStream();
            } catch (AssertionFailedError e) {
                log("failed");
                failures++;
            }
        }
        assertEquals(0, failures);
    }

    public void ignore_testExtract() throws Exception {
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

    public void ignore_testValidate() throws Exception {
        assertFalse(VorbisEncoder.validate(new File("/does/not/exist")));
        assertFalse(VorbisEncoder.validate(prepareAsset(SHORT_MONO_WAV)));
        assertTrue(VorbisEncoder.validate(prepareAsset(MED_TEST_OGG)));
        assertTrue(VorbisEncoder.validate(prepareAsset(SHORT_TEST_OGG)));
        assertFalse(VorbisEncoder.validate(prepareAsset(SHORT_TEST_NO_EOS_OGG)));
        assertTrue(VorbisEncoder.validate(prepareAsset(CHAINED_OGG)));
    }




    public void ignore_testWrite() throws Exception {
        File out = externalPath("testWrite.ogg");
        VorbisEncoder enc = new VorbisEncoder(out, "w", 1l, 44100l, 0.5f);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        int ret = enc.write(buffer, 300);
        assertEquals(300, ret);
        enc.pause();
        enc.release();
    }

    private VorbisEncoder createEncoder(File path, AudioConfig config) throws EncoderException {
        return new VorbisEncoder(path, "w", config.channels, config.sampleRate, EncoderOptions.DEFAULT.quality);
    }

    public File createWavFile(int length) throws IOException {
        File tmp = externalPath("wavefile.wav");
        WavHeader.writeHeader(tmp, length);
        if (length > 0) {
            RandomAccessFile rf = new RandomAccessFile(tmp, "rw");
            rf.setLength(length);
            rf.seek(length-1);
            rf.write(42);
            rf.close();
        }
        return tmp;
    }

    private long profile(Profileable runnable) throws Exception {
        long nativeBefore, nativeAfter;
        long globalBefore, globalAfter;

        nativeBefore = Debug.getNativeHeapAllocatedSize();
        globalBefore = Debug.getGlobalAllocCount();

        if (PROFILE)  {
            Debug.startMethodTracing();
            Debug.startNativeTracing();
        }
        Thread.sleep(4000);
        // encode it
        final long start = SystemClock.uptimeMillis();
        runnable.run();
        final long stop = SystemClock.uptimeMillis();


        if (PROFILE) {
            Debug.stopMethodTracing();
            Debug.stopNativeTracing();

        }
        nativeAfter = Debug.getNativeHeapAllocatedSize();
        globalAfter = Debug.getGlobalAllocCount();

        log("native before %d, after %d, delta %d", nativeBefore, nativeAfter, nativeAfter - nativeBefore);
        log("global before %d, after %d, delta %d", globalBefore, globalAfter, globalAfter - globalBefore);
        return stop - start;
    }

    private double encodeWav(String file, int expectedDuration, final EncoderOptions options) throws Exception {
        assertEquals("need writable external storage",
                Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);

        final InputStream in = testAssets().open(file);
        assertNotNull(in);
        WavHeader wavHeader = new WavHeader(in, true);

        final String ogg = file.replace(".wav", ".ogg");
        final File out = externalPath(ogg);


        final long duration = profile(new Profileable() {
            @Override
            public void run() throws IOException {
                VorbisEncoder.encodeWav(in, out, options);
            }
        });

        final double factor = (double) duration / (double) wavHeader.getDuration();
        final boolean mono = wavHeader.getNumChannels() == 1;
        log("encoded '%s' in quality %f in %d ms, factor %.2f (%s)", file, options.quality, duration,
                factor,
                mono ? "mono" : "stereo");

        assertTrue(
                String.format("Encoder did not produce valid ogg file (check %s with oggz-validate)", out.getAbsolutePath())
                , VorbisEncoder.validate(out));

        checkAudioFile(out, expectedDuration);

        assertTrue(String.format("encoding took more than 5x (%.2f)", factor), factor < 5 * wavHeader.getNumChannels());

        return factor;
    }

    private double encodeVorbis(String file, double expectedDuration, final EncoderOptions opts) throws Exception {
        final File ogg = prepareAsset(file);
        final File ogg_output = externalPath(file + "-re-encoded.ogg");

        final long encodeTime = profile(new Profileable() {
            @Override
            public void run() throws Exception {
                VorbisEncoder.encodeVorbis(ogg, ogg_output, opts);
            }
        });
        assertTrue(ogg_output.exists());
        assertTrue(ogg_output.length() > 0);
        assertTrue(VorbisEncoder.validate(ogg_output));
        checkVorbisDuration(ogg_output, expectedDuration);
        final double factor = (double) encodeTime / expectedDuration;
        log("encoded '%s' in quality %f in %d ms, factor %.2f", file, opts.quality, encodeTime, factor);
        return factor;
    }

    private static interface Profileable {
        void run() throws Exception;
    }
}
