package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.utils.IOUtils;

import android.content.Context;

import javax.inject.Inject;
import java.io.File;

class DiskCacheMigration implements Migration {

    private final Context context;

    @Inject
    DiskCacheMigration(Context context) {
        this.context = context;
    }

    @Override
    public void applyMigration() {
        final File old_cache = IOUtils.createExternalStorageDir(context, ".lrucache");
        if (old_cache != null && old_cache.exists()) {
            IOUtils.deleteDir(old_cache);
        }
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 158;
    }
}
