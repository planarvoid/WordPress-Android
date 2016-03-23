package com.soundcloud.android.robolectric;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.robolectric.shadows.ScShadowParcel;
import com.soundcloud.android.robolectric.shadows.ShadowArrayMap;
import com.soundcloud.android.robolectric.shadows.ShadowBaseBundle;
import com.soundcloud.android.robolectric.shadows.ShadowMediaPlayer;
import com.soundcloud.android.robolectric.shadows.ShadowNativeAmplitudeAnalyzer;
import com.soundcloud.android.robolectric.shadows.ShadowSCAccountManager;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.res.RobolectricPackageManager;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

@Deprecated
public class DefaultTestRunner extends RobolectricTestRunner {
    public static TestApplication application;

    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass, SoundCloudTestRunner.getRobolectricConfig());

        // remove native calls + replace with shadows
        addClassOrPackageToInstrument("com.soundcloud.android.creators.record.jni.NativeAmplitudeAnalyzer");
    }

    @Override
    protected TestApplication createApplication() {
        return new TestApplication();
    }

    @Override
    public void beforeTest(Method method) {
        application = (TestApplication) Robolectric.application;
        ShadowApplication shadowApplication = Robolectric.shadowOf(application);
        shadowApplication.setPackageName(robolectricConfig.getPackageName());
        shadowApplication.setPackageManager(new RobolectricPackageManager(application, robolectricConfig));
        application.onCreate();
    }

    @Override
    public void prepareTest(Object test) {
        super.prepareTest(test);
        MockitoAnnotations.initMocks(test);
    }

    @Override
    protected void resetStaticState() {
        ShadowVorbisEncoder.reset();
        ShadowNativeAmplitudeAnalyzer.reset();
    }

    @Override
    protected boolean globalI18nStrictEnabled() {
        return true;
    }

    @Override
    protected void bindShadowClasses() {
        Robolectric.bindShadowClass(ShadowNativeAmplitudeAnalyzer.class);
        Robolectric.bindShadowClass(ShadowMediaPlayer.class);
        Robolectric.bindShadowClass(ShadowSCAccountManager.class);
        Robolectric.bindShadowClass(ScShadowParcel.class);
        Robolectric.bindShadowClass(ShadowArrayMap.class);
        Robolectric.bindShadowClass(ShadowBaseBundle.class);
    }

}
