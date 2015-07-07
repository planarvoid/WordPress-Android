package com.soundcloud.android.robolectric;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.robolectric.shadows.ScShadowParcel;
import com.soundcloud.android.robolectric.shadows.ShadowArrayMap;
import com.soundcloud.android.robolectric.shadows.ShadowBaseBundle;
import com.soundcloud.android.robolectric.shadows.ShadowMediaPlayer;
import com.soundcloud.android.robolectric.shadows.ShadowNativeAmplitudeAnalyzer;
import com.soundcloud.android.robolectric.shadows.ShadowSCAccountManager;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.provider.ScContentProvider;
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

@Deprecated
public class DefaultTestRunner extends RobolectricTestRunner {
    public static TestApplication application;

    private static final File MANIFEST = new File("../app/AndroidManifest.xml");
    private static final File RESOURCES = new File("../app/res");
    private static final File ASSETS = new File("../app/assets");

    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(MANIFEST, RESOURCES, ASSETS));

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
        ShadowApplication shadowApplication = shadowOf(application);
        shadowApplication.setPackageName(robolectricConfig.getPackageName());
        shadowApplication.setPackageManager(new RobolectricPackageManager(application, robolectricConfig));
        application.onCreate();
        // delegate content provider methods
        ContentProvider provider = new ScContentProvider(new DatabaseManager(application));
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
        if (SoundCloudApplication.sModelManager != null) {
            SoundCloudApplication.sModelManager.clear();
        }
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
