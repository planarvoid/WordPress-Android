package com.soundcloud.android.tests.creators;

import com.soundcloud.android.creators.record.AmplitudeData;
import com.soundcloud.android.framework.annotation.NonUiTest;
import com.soundcloud.android.tests.ScAndroidTest;

import java.io.File;
import java.util.Arrays;

@NonUiTest
public class AmplitudeDataTest extends ScAndroidTest {

    public void ignore_testStoreAndReadAmplitudeData() throws Exception {
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
