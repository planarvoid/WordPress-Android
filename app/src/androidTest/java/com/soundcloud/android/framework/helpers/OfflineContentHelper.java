package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.Consts;
import com.soundcloud.android.framework.IntegrationTestsFixtures;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

public class OfflineContentHelper {
    private static final File OFFLINE_DIR = new File(Consts.FILES_PATH, "offline");

    private final IntegrationTestsFixtures testsFixtures;

    public OfflineContentHelper() {
        testsFixtures = new IntegrationTestsFixtures();
    }

    public int offlineFilesCount() {
        return IOUtils.nullSafeListFiles(OFFLINE_DIR, null).length;
    }

    public void clearOfflineContent(Context context) {
        // remove metadata - not sure how to do it differently
        testsFixtures.clearOfflineContent(context);
        // remove actual files
        IOUtils.cleanDir(OFFLINE_DIR);
    }

    public void addFakeOfflineTrack(Context context, Urn track, int sizeInMB) throws IOException {
        testsFixtures.insertOfflineTrack(context, track);
        IOUtils.mkdirs(OFFLINE_DIR);

        final File file = new File(OFFLINE_DIR, track.toEncodedString());
        file.createNewFile();

        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(sizeInMB * 1024L * 1024L);
    }
}
