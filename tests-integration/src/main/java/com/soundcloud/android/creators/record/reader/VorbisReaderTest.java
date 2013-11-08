package com.soundcloud.android.creators.record.reader;

import static com.soundcloud.android.tests.AudioTestCase.MED_TEST_OGG;

import com.soundcloud.android.tests.NonUiTest;
import com.soundcloud.android.tests.ScAndroidTestCase;

import java.io.File;
import java.io.IOException;

@NonUiTest
public class VorbisReaderTest extends ScAndroidTestCase {

    public void ignore_testDuration() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        VorbisReader file = new VorbisReader(ogg);
        assertEquals(18949, file.getDuration());
    }

    public void ignore_testSeekAndGetPosion() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        VorbisReader file = new VorbisReader(ogg);

        assertEquals(0, file.getPosition());
        file.seek(5000);
        assertEquals(5000, file.getPosition());
    }

    public void ignore_testSeekShouldThrowIOExceptionIfInvalidParameter() throws Exception {
        File ogg = prepareAsset(MED_TEST_OGG);
        VorbisReader file = new VorbisReader(ogg);
        try {
            file.seek(-2000);
            fail("expected exception");
        } catch (IOException e) {
        }
    }
}
