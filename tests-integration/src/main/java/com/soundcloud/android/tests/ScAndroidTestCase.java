package com.soundcloud.android.tests;

import com.soundcloud.android.utils.IOUtils;

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
public abstract class ScAndroidTestCase extends AndroidTestCase {
    public static final String TEST_DIR = "sc-tests";

    protected AssetManager testAssets() {
        try {
            return getContext()
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

        File out = externalPath(name);
        IOUtils.copy(in, out);
        return out;
    }

    protected File externalPath(String name) {
        checkStorage();
        File file = new File(new File(Environment.getExternalStorageDirectory(), TEST_DIR), name);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) fail("could not create "+file.getParentFile());
        if (file.exists() && !file.delete()) fail("could not delete " + file);
        return file;
    }

    protected void checkStorage() {
        assertEquals("need writable external storage",
                Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);

    }

    protected void log(String s, Object... args) {
        Log.d(getClass().getSimpleName(), String.format(Locale.ENGLISH, s, args));
    }

    protected String newFilename(String name, String suffix) {
        int dot = name.lastIndexOf('.');
        if (dot != -1 && dot+1 < name.length()) {
            return name.substring(0, dot) + suffix + name.substring(dot, name.length());
        } else {
            return name + suffix;
        }
    }
}
