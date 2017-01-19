package com.soundcloud.android.framework.runner;

import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

@SuppressWarnings("UnusedDeclaration")
public class Runner extends RandomizingRunner {
    public static final String TEST_DIR = "sc-tests";
    private static final long MIN_BYTES_FREE = (1024 * 1024) * 30;

    @Override
    public void onStart() {
        checkExternalStorage();
        checkFreeSpace(getContext());
        createDirs(getContext());
        super.onStart();
    }

    public static void createDirs(Context context) {
        File testDir = IOUtils.getExternalStorageDir(context, TEST_DIR);
        File externalStorageDir = IOUtils.getExternalStorageDir(context);
        if (externalStorageDir != null) {
            IOUtils.cleanDir(externalStorageDir);
        }

        if (!IOUtils.mkdirs(testDir)) {
            throw new AssertionError("Could not create " + testDir);
        }
    }

    public static void checkFreeSpace(Context context) {
        checkExternalStorage();

        final long bytesFree = IOUtils.getSpaceLeft(IOUtils.getExternalStorageDir(context));
        if (bytesFree < MIN_BYTES_FREE) {
            throw new AssertionError("not enough external storage: (" + bytesFree + "<" + MIN_BYTES_FREE + ")");
        }
    }

    public static void checkExternalStorage() {
        if (!IOUtils.isSDCardAvailable()) {
            throw new AssertionError("need writable external storage (state=" + Environment.getExternalStorageState() + ")");
        }
    }
}
