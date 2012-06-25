package com.soundcloud.android.audio;

import static com.soundcloud.android.tests.AudioTestCase.MED_TEST_OGG;

import com.soundcloud.android.tests.ScAndroidTestCase;

import java.io.File;
import java.io.IOException;

public class VorbisFileTest extends ScAndroidTestCase {

    public void testDuration() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        VorbisFile file = new VorbisFile(ogg);
        assertEquals(18786, file.getDuration());
    }

    public void testSeekAndGetPosion() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        VorbisFile file = new VorbisFile(ogg);

        assertEquals(0, file.getPosition());
        file.seek(5000);
        assertEquals(5000, file.getPosition());
    }

    public void testSeekShouldThrowIOExceptionIfInvalidParameter() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        VorbisFile file = new VorbisFile(ogg);
        try {
            file.seek(-2000);
            fail("expected exception");
        } catch (IOException e) {
        }
    }
}
