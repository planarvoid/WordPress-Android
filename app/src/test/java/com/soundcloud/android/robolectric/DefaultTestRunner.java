package com.soundcloud.android.robolectric;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.robolectric.shadows.*;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.res.RobolectricPackageManager;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.util.SQLiteMap;
import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;

import android.content.ContentProvider;

import java.io.File;
import java.lang.reflect.Method;

/**
 * In order to use a file-based test database, annotate your test classes with
 * <code>
 *     \@DatabaseConfig.UsingDatabaseMap(DefaultTestRunner.FileDatabaseMap.class)
 * </code>.
 */
@Deprecated
public class DefaultTestRunner extends RobolectricTestRunner {
    public static TestApplication application;

    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass,new RobolectricConfig(new File("../app")));

        // remove native calls + replace with shadows
        addClassOrPackageToInstrument("com.soundcloud.android.creators.record.jni.VorbisEncoder");
        addClassOrPackageToInstrument("com.soundcloud.android.creators.record.jni.VorbisDecoder");
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
        TestHelper.setSdkVersion(0);
    }

    @Override
    protected boolean globalI18nStrictEnabled() {
        return true;
    }

    @Override
    protected void bindShadowClasses() {
        Robolectric.bindShadowClass(ShadowVorbisEncoder.class);
        Robolectric.bindShadowClass(ShadowNativeAmplitudeAnalyzer.class);
        Robolectric.bindShadowClass(ShadowMediaPlayer.class);
        Robolectric.bindShadowClass(ShadowSCAccountManager.class);
        Robolectric.bindShadowClass(ScShadowParcel.class);
        Robolectric.bindShadowClass(ShadowArrayMap.class);
        Robolectric.bindShadowClass(ShadowBaseBundle.class);
        Robolectric.bindShadowClass(ScShadowSQLiteDatabase.class);
    }

    public static class FileDatabaseMap extends SQLiteMap {
        @Override
        public String getConnectionString() {
            return "jdbc:sqlite:tests-" + System.currentTimeMillis() +".sqlite";
        }
    }
}
