package com.soundcloud.android.jni;

import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.tests.AudioTestCase;

import android.test.suitebuilder.annotation.LargeTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@LargeTest
public class VorbisDecoderTest extends AudioTestCase {
    public static final String MED_TEST_OGG = "audio/med_test.ogg";

    public void testDecodeToFile() throws Exception {
        File wav = decode(MED_TEST_OGG);
        assertTrue("file does not exist", wav.exists());

        WavHeader header = new WavHeader(new FileInputStream(wav));
        assertEquals(16, header.getBitsPerSample());
        assertEquals(2, header.getNumChannels());
        assertEquals(44100, header.getSampleRate());
        assertEquals(3313920, header.getNumBytes());

        checkAudioFile(wav, 18786);
    }

    public void testDecodeBuffer() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));

        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        int n, total = 0;
        while ((n = decoder.decode(bb, bb.capacity())) > 0) {
            total += n;
        }
        assertEquals("non-zero return code: "+n, 0, n);
        assertEquals(3313920, total);

        decoder.release();
    }

    public void testPcmSeek() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));
        assertEquals(0, decoder.pcmSeek(44100 * 10));

        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        int n, total = 0;
        while ((n = decoder.decode(bb, bb.capacity())) > 0) {
            total += n;
        }
        assertEquals("non-zero return code: "+n, 0, n);
        assertEquals(1549920, total);

        decoder.release();
    }


    public void testTimeSeek() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));
        assertEquals(0, decoder.timeSeek(10d));

        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        int n, total = 0;
        while ((n = decoder.decode(bb, bb.capacity())) > 0) {
            total += n;
        }
        assertEquals("non-zero return code: "+n, 0, n);
        assertEquals(1549920, total);

        decoder.release();
    }

    public void testTimeTell() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));
        assertEquals(0, decoder.timeSeek(10d));
        assertEquals(10d, decoder.timeTell());

        // decode some stuff
        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        int read = 0;
        do {
            read += decoder.decode(bb, bb.capacity());
        }  while (read < 44100 * 2 * 2 /* around 1sec worth of data */);

        // and check time
        assertEquals(11.004807256235827d, decoder.timeTell(), 0.001d);

        decoder.release();
    }


    public void testGetInfo() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));
        VorbisInfo info = decoder.getInfo();

        assertNotNull(info);
        assertEquals(44100, info.sampleRate);
        assertEquals(2, info.channels);
        assertEquals(828480,  info.numSamples);
        assertEquals(330819,  info.bitrate, 10);
        assertEquals(18.78639455782313d, info.duration, 0.001d);

        decoder.release();
    }

    public void testRelease() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));
        assertEquals(0, decoder.getState());
        decoder.release();
        assertEquals(-1, decoder.getState());
    }

    public void testRaisesExceptionOnInitialisationError() throws Exception {
        try {
            new VorbisDecoder(prepareAsset("audio/med_test.wav"));
            fail("decoder did not throw exception with bad data");
        } catch (IOException expected) {
            assertEquals("Error initializing decoder: OV_ENOTVORBIS", expected.getMessage());
        }
    }

    private File decode(String in) throws IOException {
        File ogg = prepareAsset(in);
        VorbisDecoder decoder = new VorbisDecoder(ogg);

        File wav = externalPath(in.replace(".ogg", ".wav"));

        final long start = System.currentTimeMillis();
        decoder.decodeToFile(wav);
        log("decoded %s in %d ms", in, (System.currentTimeMillis() - start));
        decoder.release();
        return wav;
    }
}
