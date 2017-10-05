package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.playback.skippy.SkippyCache;
import com.soundcloud.android.playback.skippy.SkippyConfiguration;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.File;

class StreamCacheMigration implements Migration {

    private static final String TAG = "StreamCacheMigration";

    private final SkippyCache skippyCache;

    @Inject
    StreamCacheMigration(SkippyConfiguration skippyConfiguration) {
        this.skippyCache = skippyConfiguration.getCache();
    }

    @Override
    public void applyMigration() {
        File streamCacheDirectory = skippyCache.directory();

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
