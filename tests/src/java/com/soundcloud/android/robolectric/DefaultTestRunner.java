package com.soundcloud.android.robolectric;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.runners.model.InitializationError;

import android.content.ContentProvider;

import java.io.File;
import java.lang.reflect.Method;

public class DefaultTestRunner extends RobolectricTestRunner {
    public static TestApplication application;

    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File(".")) {
            @Override public String getApplicationName() {
                return TestApplication.class.getSimpleName();
            }
        });

        // remove native calls + replace with shadows
        addClassOrPackageToInstrument("com.soundcloud.android.jni.VorbisEncoder");
        addClassOrPackageToInstrument("com.soundcloud.android.jni.VorbisDecoder");
    }

    @Override
    public void beforeTest(Method method) {
        application = (TestApplication) Robolectric.application;
        // delegate content provider methods
        ContentProvider provider = new ScContentProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(ScContentProvider.AUTHORITY, provider);

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
