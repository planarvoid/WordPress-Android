package com.soundcloud.android.tests;

import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Base test case for resource handling.
 */
public abstract class ScAndroidTest extends AndroidTestCase {
    private static final String TEST_DIR = "sc-tests";

    private AssetManager testAssets() {
        return testAssets(getContext());
    }

    private static AssetManager testAssets(Context context) {
        try {
            return context
                    .getPackageManager()
                    .getResourcesForApplication("com.soundcloud.android.tests")
                    .getAssets();
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected File prepareAsset(String name) throws IOException {
        checkStorage();

        InputStream in = testAssets().open(name);
        assertNotNull(in);

        File out = externalPath(getContext(), name);
        IOUtils.copy(in, out);
        return out;
    }

    private File externalPath(Context context, String name) {
        checkStorage();
        File file = new File(IOUtils.createExternalStorageDir(context, TEST_DIR), name);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            fail("could not create " + file.getParentFile());
        }
        if (file.exists() && !file.delete()) {
            fail("could not delete " + file);
        }
        return file;
    }

    private void checkStorage() {
        assertEquals("need writable external storage",
                     Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);
    }

    protected void log(String s, Object... args) {
        Log.d(getClass().getSimpleName(), String.format(Locale.ENGLISH, s, args));
    }

}
