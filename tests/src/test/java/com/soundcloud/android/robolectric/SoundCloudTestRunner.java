package com.soundcloud.android.robolectric;


import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.robolectric.shadows.ScShadowParcel;
import com.soundcloud.android.robolectric.shadows.ShadowSherlockFragment;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.lang.reflect.Method;

public class SoundCloudTestRunner extends RobolectricTestRunner {

    public SoundCloudTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File("../app")));
    }

    @Override
    public void prepareTest(Object test) {
        super.prepareTest(test);
        MockitoAnnotations.initMocks(test);
    }

    @Override
    public void beforeTest(Method method) {
        super.beforeTest(method);

        // until we have a DI framework we have to set this instance to avoid NPEs
        SoundCloudApplication.instance = Robolectric.application;

        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(Robolectric.application));
    }

    @Override
    protected void bindShadowClasses() {
        Robolectric.bindShadowClass(ScShadowParcel.class);
        Robolectric.bindShadowClass(ShadowSherlockFragment.class);
    }
}
