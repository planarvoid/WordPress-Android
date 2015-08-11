package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.IOUtils;

import javax.inject.Inject;

class StreamCacheMigration implements Migration {

    @Inject
    StreamCacheMigration() {
    }

    @Override
    public void applyMigration() {
        IOUtils.cleanDirs(Consts.EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY, Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY);
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 254;
    }
}
