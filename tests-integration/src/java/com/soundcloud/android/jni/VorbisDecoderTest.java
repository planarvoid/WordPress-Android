package com.soundcloud.android.jni;

import com.soundcloud.android.record.WaveHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VorbisDecoderTest extends AudioTest {
    public static final String MED_TEST_OGG = "audio/med_test.ogg";

    public void testDecodeToFile() throws Exception {
        File wav = decode(MED_TEST_OGG);
        assertTrue("file does not exist", wav.exists());

        WaveHeader header = new WaveHeader(new FileInputStream(wav));
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

    public void testGetInfo() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));
        Info info = decoder.getInfo();

        assertNotNull(info);
        assertEquals(44100, info.sampleRate);
        assertEquals(2, info.channels);
        assertEquals(828480, info.numSamples);

        decoder.release();
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
