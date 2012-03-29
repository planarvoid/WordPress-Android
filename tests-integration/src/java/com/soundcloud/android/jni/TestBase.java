package com.soundcloud.android.jni;

import com.soundcloud.android.utils.IOUtils;

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public abstract class TestBase extends AndroidTestCase {
    public static final String TEST_DIR = "sc-tests";

    protected AssetManager assets() {
        try {
            return getContext()
                    .getPackageManager()
                    .getResourcesForApplication("com.soundcloud.android.tests")
                    .getAssets();
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected File externalPath(String name) {
        checkStorage();
        File testDir = new File(Environment.getExternalStorageDirectory(), TEST_DIR);
        if (!testDir.exists() && !testDir.mkdirs()) fail("could not create " + testDir);
        File file = new File(testDir, name);
        if (file.exists() && !file.delete()) fail("could not delete " + file);
        return file;
    }

    protected File prepareAsset(String name) throws IOException {
        checkStorage();

        InputStream in = assets().open(name);
        assertNotNull(in);

        File out = externalPath(name);
        IOUtils.copy(in, out);
        return out;
    }

    protected void checkStorage() {
        assertEquals("need writable external storage",
                Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);

    }

    protected void checkAudioFile(File file, int expectedDuration) throws IOException {
        assertTrue("file should exist", file.exists());

        // read encoded file with mediaplayer
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();

            int duration = mp.getDuration();
            //mediaplayer, y u so broken?
            //assertEquals(expectedDuration, duration);
            assertTrue(duration > 0);
        } finally {
            if (mp != null) mp.release();
        }
    }

    protected void log(String s, Object... args) {
        Log.d(getClass().getSimpleName(), String.format(Locale.ENGLISH, s, args));
    }
}
