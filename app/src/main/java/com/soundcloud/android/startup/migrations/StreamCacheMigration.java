package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.playback.StreamCacheConfig;
import com.soundcloud.android.utils.IOUtils;

import javax.inject.Inject;

class StreamCacheMigration implements Migration {

    private final StreamCacheConfig streamCacheConfig;

    @Inject
    StreamCacheMigration(StreamCacheConfig streamCacheConfig) {
        this.streamCacheConfig = streamCacheConfig;
    }

    @Override
    public void applyMigration() {
        IOUtils.cleanDirs(Consts.EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY, streamCacheConfig.getStreamCacheDirectory());
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 254;
    }
}
