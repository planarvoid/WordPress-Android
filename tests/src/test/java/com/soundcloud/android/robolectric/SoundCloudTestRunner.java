package com.soundcloud.android.robolectric;


import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;

public class SoundCloudTestRunner extends RobolectricTestRunner {

    public SoundCloudTestRunner(Class testClass) throws InitializationError {
        super(testClass,new RobolectricConfig(new File("../app")));
    }
}
