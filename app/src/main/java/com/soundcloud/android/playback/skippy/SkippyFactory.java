package com.soundcloud.android.playback.skippy;

import com.soundcloud.android.Consts;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.playback.CacheConfig;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SkippyFactory {

    private static final String KEY_PREFERENCE_NAME = "skippy_cache";
    private static final int PROGRESS_INTERVAL_MS = 500;
    private static final int BUFFER_DURATION_MS = (int) TimeUnit.SECONDS.toMillis(90);
    private static final boolean ALL_TRACKS_CACHE = false;
    private static final int CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 1;

    private final ApplicationProperties applicationProperties;
    private final CryptoOperations cryptoOperations;
    private final TelphonyBasedCountryProvider countryProvider;

    @Inject
    SkippyFactory(CryptoOperations cryptoOperations, ApplicationProperties applicationProperties,
                  TelphonyBasedCountryProvider countryProvider) {
        this.cryptoOperations = cryptoOperations;
        this.applicationProperties = applicationProperties;
        this.countryProvider = countryProvider;
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
                CacheConfig.getCacheSize(countryProvider.getCountryCode()),
                CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE,
                Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY.getAbsolutePath(),
                cryptoOperations.getKeyOrGenerateAndStore(KEY_PREFERENCE_NAME),
                !applicationProperties.isReleaseBuild(),
                ALL_TRACKS_CACHE
        );
    }

}
