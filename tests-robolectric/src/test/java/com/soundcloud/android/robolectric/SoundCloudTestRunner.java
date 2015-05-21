package com.soundcloud.android.robolectric;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplicationModule;
import com.soundcloud.android.robolectric.shadows.ScShadowParcel;
import com.soundcloud.android.robolectric.shadows.ScShadowPreferenceActivity;
import com.soundcloud.android.robolectric.shadows.ScShadowSQLiteDatabase;
import com.soundcloud.android.robolectric.shadows.ShadowArrayMap;
import com.soundcloud.android.robolectric.shadows.ShadowBase64;
import com.soundcloud.android.robolectric.shadows.ShadowBaseBundle;
import com.soundcloud.android.robolectric.shadows.ShadowRecyclerViewHolder;
import com.soundcloud.android.robolectric.shadows.ShadowSystemClock;
import com.soundcloud.android.robolectric.shadows.ShadowV4CursorAdapter;
import com.soundcloud.android.robolectric.shadows.ShadowV4Fragment;
import com.soundcloud.android.robolectric.shadows.ShadowV4ListFragment;
import com.soundcloud.android.robolectric.shadows.ShadowV7PopupMenu;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.util.SQLiteMap;
import dagger.ObjectGraph;
import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.lang.reflect.Method;

/**
 * In order to use a file-based test database, annotate your test classes with
 * <code>
 *     \@DatabaseConfig.UsingDatabaseMap(SoundCloudTestRunner.FileDatabaseMap.class)
 * </code>.
 */
public class SoundCloudTestRunner extends RobolectricTestRunner {

    private static final File MANIFEST = new File("app/AndroidManifest.xml");
    private static final File RESOURCES = new File("app/res");
    private static final File ASSETS = new File("app/assets");

    public SoundCloudTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(MANIFEST, RESOURCES, ASSETS));
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
        SoundCloudApplication application = (SoundCloudApplication) Robolectric.application;
        SoundCloudApplication.instance = application;
        ObjectGraph.create(new TestApplicationModule(application)).inject(application);
    }

    @Override
    protected void resetStaticState() {
        TestHelper.setSdkVersion(robolectricConfig.getSdkVersion());
    }

    @Override
    protected void bindShadowClasses() {
        Robolectric.bindShadowClass(ScShadowParcel.class);
        Robolectric.bindShadowClass(ShadowV4Fragment.class);
        Robolectric.bindShadowClass(ShadowV4ListFragment.class);
        Robolectric.bindShadowClass(ShadowV4CursorAdapter.class);
        Robolectric.bindShadowClass(ScShadowSQLiteDatabase.class);
        Robolectric.bindShadowClass(ScShadowPreferenceActivity.class);
        Robolectric.bindShadowClass(ShadowSystemClock.class);
        Robolectric.bindShadowClass(ShadowV7PopupMenu.class);
        Robolectric.bindShadowClass(ShadowArrayMap.class);
        Robolectric.bindShadowClass(ShadowBase64.class);
        Robolectric.bindShadowClass(ShadowBaseBundle.class);
        Robolectric.bindShadowClass(ShadowVorbisEncoder.class);
        Robolectric.bindShadowClass(ShadowRecyclerViewHolder.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class FileDatabaseMap extends SQLiteMap {
        @Override
        public String getConnectionString() {
            return "jdbc:sqlite:tests-" + System.currentTimeMillis() +".sqlite";
        }
    }
}
