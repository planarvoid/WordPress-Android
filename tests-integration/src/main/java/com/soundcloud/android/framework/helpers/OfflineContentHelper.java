package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

public class OfflineContentHelper {

    private static final File OFFLINE_DIR = new File(Consts.FILES_PATH, "offline");

    public static int offlineFilesCount() {
        return IOUtils.nullSafeListFiles(OFFLINE_DIR, null).length;
    }

    public static void clearOfflineContent(Context context) {
        // remove metadata - not sure how to do it differently
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        Table.TrackDownloads.recreate(db);
        // remove actual files
        IOUtils.cleanDirectory(OFFLINE_DIR);
    }
}
