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

    public void testDecodeToFile() throws Exception {
        File wav = decode(MED_TEST_OGG);
        assertTrue("file does not exist", wav.exists());

        WavHeader header = new WavHeader(new FileInputStream(wav));
        assertEquals(16, header.getBitsPerSample());
        assertEquals(2, header.getNumChannels());
        assertEquals(44100, header.getSampleRate());
        assertEquals(3342640, header.getNumBytes());

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
        assertEquals(3342640, total);

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
        assertEquals(1578640, total);

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
        assertEquals(1578640, total);

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
        assertEquals(835660,  info.numSamples);
        assertEquals(331249,  info.bitrate, 10);
        assertEquals(18.949, info.duration, 0.001d);

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
            new VorbisDecoder(prepareAsset(MED_WAV));
            fail("decoder did not throw exception with bad data");
        } catch (IOException expected) {
            assertEquals("Error initializing decoder: OV_ENOTVORBIS", expected.getMessage());
        }
    }

    public void testShouldDecodeChainedOggFiles() throws Exception {
        VorbisDecoder dec = new VorbisDecoder(prepareAsset(CHAINED_OGG));
        VorbisInfo info = dec.getInfo();

        assertEquals(0.993d + 1.269d + 1.16d, info.duration, 0.002);

        double time = 0;
        // decode the whole thing
        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        int n, total = 0;
        while ((n = dec.decode(bb, bb.capacity())) > 0) {
            double newTime = dec.timeTell();
            assertTrue(newTime > time);
            assertEquals(time, newTime, 0.05);
            time = newTime;
            total += n;
        }
        assertEquals(301952, total);
    }

    public void testTimeTellShouldNotJump() throws Exception {
        VorbisDecoder dec = new VorbisDecoder(prepareAsset(TRIMMED_RECORDING));
        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        int n;
        double old = 0;
        while ((n = dec.decode(bb, bb.capacity())) > 0) {
            double newTime = dec.timeTell();
            assertEquals(old, newTime, 0.05);
            old = newTime;
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
