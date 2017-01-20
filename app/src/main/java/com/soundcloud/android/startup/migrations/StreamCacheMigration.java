package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.playback.StreamCacheConfig;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.File;

class StreamCacheMigration implements Migration {

    private static final String TAG = "StreamCacheMigration";

    private final StreamCacheConfig.SkippyConfig streamCacheConfig;

    @Inject
    StreamCacheMigration(StreamCacheConfig.SkippyConfig streamCacheConfig) {
        this.streamCacheConfig = streamCacheConfig;
    }

    @Override
    public void applyMigration() {
        File streamCacheDirectory = streamCacheConfig.getStreamCacheDirectory();

        if (streamCacheDirectory != null) {
            IOUtils.cleanDir(streamCacheDirectory);
        } else {
            Log.w(TAG, "streamCacheDirectory is null");
        }
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 254;
    }
}
