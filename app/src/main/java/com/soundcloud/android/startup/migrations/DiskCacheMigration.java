package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.IOUtils;

import javax.inject.Inject;
import java.io.File;

class DiskCacheMigration implements Migration {

    @Inject
    DiskCacheMigration() {
    }

    @Override
    public void applyMigration() {
        final File old_cache = new File(Consts.FILES_PATH, ".lrucache");
        if (old_cache.exists()) {
            IOUtils.deleteDir(old_cache);
        }
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 158;
    }
}
