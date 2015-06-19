package com.soundcloud.android.testsupport;

import com.soundcloud.android.BuildConfig;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.content.Context;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, application = ApplicationStub.class)
public abstract class RobolectricUnitTest {

    protected static Context context() {
        return RuntimeEnvironment.application;
    }
}
