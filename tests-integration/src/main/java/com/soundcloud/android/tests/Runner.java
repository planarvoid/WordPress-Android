package com.soundcloud.android.tests;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.IOUtils;

import android.os.Environment;
import android.test.InstrumentationTestRunner;

import java.io.File;

@SuppressWarnings("UnusedDeclaration")
public class Runner extends InstrumentationTestRunner {
    public static final String TEST_DIR = "sc-tests";

    @Override
    public void onStart() {
        File testDir = new File(Environment.getExternalStorageDirectory(), TEST_DIR);
        IOUtils.deleteDir(Consts.EXTERNAL_STORAGE_DIRECTORY);
        IOUtils.deleteDir(testDir);

        if (!IOUtils.mkdirs(testDir)) {
            throw new RuntimeException("Could not create "+testDir);
        }
        super.onStart();
    }
}
