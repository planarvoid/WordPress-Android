package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.IntegrationTestsFixtures;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class OfflineContentHelper {
    private static final String OFFLINE_DIR = "offline";

    private final IntegrationTestsFixtures testsFixtures;

    public OfflineContentHelper() {
        testsFixtures = new IntegrationTestsFixtures();
    }

    public int offlineFilesCount(Context context) {
        return IOUtils.nullSafeListFiles(IOUtils.getExternalStorageDir(context, OFFLINE_DIR), null).length;
    }

    public void clearOfflineContent(Context context) {
        // remove metadata - not sure how to do it differently
        testsFixtures.clearOfflineContent(context);
        // remove actual files
        File externalStorageDir = IOUtils.getExternalStorageDir(context, OFFLINE_DIR);
        if (externalStorageDir != null) {
            IOUtils.cleanDir(externalStorageDir);
        }
    }

    public void addFakeOfflineTrack(Context context, Urn track, int sizeInMB) throws IOException {
        testsFixtures.insertOfflineTrack(context, track);


        final File file = new File(IOUtils.getExternalStorageDir(context, OFFLINE_DIR), track.toEncodedString());
        file.createNewFile();

        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(sizeInMB * 1024L * 1024L);
    }

    public void updateOfflineTracksPolicyUpdateTime(Context context, long lastUpdateTime) {
        testsFixtures.updateLastPolicyUpdateTime(context, lastUpdateTime);
    }
}
