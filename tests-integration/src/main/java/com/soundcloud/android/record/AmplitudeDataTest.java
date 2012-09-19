package com.soundcloud.android.record;

import com.soundcloud.android.tests.ScAndroidTestCase;

import java.io.File;
import java.util.Arrays;

public class AmplitudeDataTest extends ScAndroidTestCase {

    public void testStoreAndReadAmplitudeData() throws Exception {
        AmplitudeData data = new AmplitudeData();
        data.add(1f);
        data.add(2f);
        data.add(3f);

        File dataPath = externalPath("amplitudedata");
        data.store(dataPath);
        assertTrue(dataPath.exists());

        AmplitudeData readData = AmplitudeData.fromFile(dataPath);
        assertTrue(Arrays.equals(data.get(), readData.get()));
    }
}
