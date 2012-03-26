package com.soundcloud.android.jni;

import com.soundcloud.android.utils.record.WaveHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VorbisDecoderTest extends TestBase {

    public void testDecoding() throws Exception {
        File wav = decode("med_test.ogg");
        assertTrue("file does not exist", wav.exists());

        WaveHeader header = new WaveHeader(new FileInputStream(wav));
        assertEquals(header.getBitsPerSample(), 16);
        assertEquals(header.getNumChannels(), 2);
        assertEquals(header.getSampleRate(), 44100);
        assertEquals(header.getNumBytes(), 3313920);

        checkAudioFile(wav, 18786);
    }

    private File decode(String in) throws IOException {
        VorbisDecoder decoder = new VorbisDecoder();

        File ogg = prepareAsset(in);
        File wav = externalPath(in.replace(".ogg", ".wav"));

        final long start = System.currentTimeMillis();
        decoder.decode(ogg, wav);
        log("decoded %s in %d ms", in, (System.currentTimeMillis() - start));

        return wav;
    }
}
