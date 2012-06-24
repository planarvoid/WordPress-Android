package com.soundcloud.android.tests;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.IOUtils;

import android.os.Environment;
import android.test.InstrumentationTestRunner;

import java.io.File;

@SuppressWarnings("UnusedDeclaration")
public class Runner extends InstrumentationTestRunner {
    public static final String TEST_DIR = "sc-tests";
    private static final long MIN_BYTES_FREE = (1024*1024)* 30;

    @Override
    public void onStart() {
        File testDir = new File(Environment.getExternalStorageDirectory(), TEST_DIR);
        IOUtils.deleteDir(Consts.EXTERNAL_STORAGE_DIRECTORY);
        IOUtils.deleteDir(testDir);

        if (!IOUtils.mkdirs(testDir)) {
            throw new RuntimeException("Could not create "+testDir);
        }

        final long bytesFree = IOUtils.getSpaceLeft(Environment.getExternalStorageDirectory());
        if (bytesFree < MIN_BYTES_FREE) {
            throw new RuntimeException("not enough external storage: ("+bytesFree+"<"+MIN_BYTES_FREE+")");
        }
        super.onStart();
    }
}
