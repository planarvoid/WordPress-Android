package com.soundcloud.android.jni;

import com.soundcloud.android.record.WaveHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VorbisDecoderTest extends AudioTest {
    public static final String MED_TEST_OGG = "audio/med_test.ogg";

    public void testDecoding() throws Exception {
        File wav = decode(MED_TEST_OGG);
        assertTrue("file does not exist", wav.exists());

        WaveHeader header = new WaveHeader(new FileInputStream(wav));
        assertEquals(16, header.getBitsPerSample());
        assertEquals(2, header.getNumChannels());
        assertEquals(44100, header.getSampleRate());
        assertEquals(3313920, header.getNumBytes());

        checkAudioFile(wav, 18786);
    }

    public void testGetInfo() throws Exception {
        VorbisDecoder decoder = new VorbisDecoder(prepareAsset(MED_TEST_OGG));

        Info info = decoder.getInfo();

        assertNotNull(info);
        assertEquals(44100, info.sampleRate);
        assertEquals(2, info.channels);
        assertEquals(828480, info.numSamples);
    }

    private File decode(String in) throws IOException {
        File ogg = prepareAsset(in);
        VorbisDecoder decoder = new VorbisDecoder(ogg);

        File wav = externalPath(in.replace(".ogg", ".wav"));

        final long start = System.currentTimeMillis();
        decoder.decodeToFile(wav);
        log("decoded %s in %d ms", in, (System.currentTimeMillis() - start));

        return wav;
    }


}
