package com.soundcloud.android.playback.service.skippy;

import com.soundcloud.android.Consts;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.skippy.Skippy;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SkippyFactory {

    private static final String KEY_PREFERENCE_NAME = "skippy_cache";
    private static final int PROGRESS_INTERVAL_MS = 500;
    private static final int BUFFER_DURATION_MS = (int) TimeUnit.SECONDS.toMillis(60);
    private static final boolean ALL_TRACKS_CACHE = false;
    private static final int MAX_CACHE_SIZE_BYTES = 300 * 1024 * 1024;
    private static final int CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 10;

    private final ApplicationProperties applicationProperties;
    private final CryptoOperations cryptoOperations;

    @Inject
    SkippyFactory(CryptoOperations cryptoOperations, ApplicationProperties applicationProperties) {
        this.cryptoOperations = cryptoOperations;
        this.applicationProperties = applicationProperties;
    }


    public Skippy create() {
        return new Skippy();
    }

    public Skippy create(Skippy.PlayListener listener) {
        return new Skippy(listener);
    }

    public Skippy.Configuration createConfiguration() {
        return new Skippy.Configuration(
                PROGRESS_INTERVAL_MS,
                BUFFER_DURATION_MS,
                MAX_CACHE_SIZE_BYTES,
                CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE,
                Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY.getAbsolutePath(),
                cryptoOperations.getKeyOrGenerateAndStore(KEY_PREFERENCE_NAME),
                applicationProperties.useVerboseLogging(),
                ALL_TRACKS_CACHE
        );
    }

}
