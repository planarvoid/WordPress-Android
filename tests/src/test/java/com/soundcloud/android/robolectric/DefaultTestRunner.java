package com.soundcloud.android.robolectric;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.res.RobolectricPackageManager;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;

import android.content.ContentProvider;

import java.io.File;
import java.lang.reflect.Method;

public class DefaultTestRunner extends RobolectricTestRunner {
    public static TestApplication application;

    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass,  new RobolectricConfig(new File("../app")));

        // remove native calls + replace with shadows
        addClassOrPackageToInstrument("com.soundcloud.android.jni.VorbisEncoder");
        addClassOrPackageToInstrument("com.soundcloud.android.jni.VorbisDecoder");
    }


    @Override
    protected TestApplication createApplication() {
        return new TestApplication();
    }

    @Override
    public void beforeTest(Method method) {
        application = (TestApplication) Robolectric.application;
        ShadowApplication shadowApplication = shadowOf(application);
        shadowApplication.setPackageName(robolectricConfig.getPackageName());
        shadowApplication.setPackageManager(new RobolectricPackageManager(application, robolectricConfig));
        application.onCreate();

        // delegate content provider methods
        ContentProvider provider = new ScContentProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(ScContentProvider.AUTHORITY, provider);
    }

    @Override
    public void prepareTest(Object test) {
        super.prepareTest(test);
        MockitoAnnotations.initMocks(test);
    }

    @Override
    protected void resetStaticState() {
        SoundCloudApplication.TRACK_CACHE.clear();
        SoundCloudApplication.USER_CACHE.clear();
    }

    @Override
    protected boolean globalI18nStrictEnabled() {
        return true;
    }

    @Override
    protected void bindShadowClasses() {
        Robolectric.bindShadowClass(ShadowVorbisEncoder.class);
    }
}
