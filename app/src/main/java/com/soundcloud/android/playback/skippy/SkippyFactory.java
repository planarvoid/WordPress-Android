package com.soundcloud.android.playback.skippy;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.playback.StreamCacheConfig;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.skippy.Skippy;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SkippyFactory {

    private static final String KEY_PREFERENCE_NAME = "skippy_cache";
    private static final int PROGRESS_INTERVAL_MS = 500;
    private static final int BUFFER_DURATION_MS = (int) TimeUnit.SECONDS.toMillis(90);
    private static final int PRELOADER_BUFFER_DURATION_MS = (int) TimeUnit.SECONDS.toMillis(10);
    private static final Skippy.CacheRestriction USE_CACHE_ALWAYS = Skippy.CacheRestriction.NONE;

    private final Context context;
    private final ApplicationProperties applicationProperties;
    private final CryptoOperations cryptoOperations;
    private final StreamCacheConfig cacheConfig;

    @Inject
    SkippyFactory(Context context, CryptoOperations cryptoOperations,
                  ApplicationProperties applicationProperties,
                  StreamCacheConfig cacheConfig) {
        this.context = context;
        this.cryptoOperations = cryptoOperations;
        this.applicationProperties = applicationProperties;
        this.cacheConfig = cacheConfig;
    }

    public Skippy create() {
        return new Skippy(context);
    }

    public Skippy create(Skippy.PlayListener listener) {
        return new Skippy(context, listener);
    }

    public Skippy.Configuration createConfiguration() {
        return getConfiguration(BUFFER_DURATION_MS);
    }

    public Skippy.Configuration createPreloaderConfiguration() {
        return getConfiguration(PRELOADER_BUFFER_DURATION_MS);
    }

    @NonNull
    private Skippy.Configuration getConfiguration(int bufferDurationMs) {
        return new Skippy.Configuration(
                PROGRESS_INTERVAL_MS,
                bufferDurationMs,
                cacheConfig.getStreamCacheSize(),
                cacheConfig.getStreamCacheMinFreeSpaceAvailablePercentage(),
                cacheConfig.getStreamCacheDirectory().getAbsolutePath(),
                cryptoOperations.getKeyOrGenerateAndStore(KEY_PREFERENCE_NAME),
                !applicationProperties.isReleaseBuild(),
                USE_CACHE_ALWAYS
        );
    }

}
