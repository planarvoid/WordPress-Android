package com.soundcloud.android.robolectric;


import com.soundcloud.android.SoundCloudApplication;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.lang.reflect.Method;

public class SoundCloudTestRunner extends RobolectricTestRunner {

    public SoundCloudTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File("../app")));
    }

    @Override
    public void beforeTest(Method method) {
        super.beforeTest(method);

        // until we have a DI framework we have to set this instance to avoid NPEs
        SoundCloudApplication.instance = Robolectric.application;
    }
}
