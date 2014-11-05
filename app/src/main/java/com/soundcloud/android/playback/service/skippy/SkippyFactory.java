package com.soundcloud.android.playback.service.skippy;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.skippy.Skippy;

import android.content.SharedPreferences;
import android.content.res.Resources;

import javax.inject.Inject;

public class SkippyFactory {

    private static final String KEY_PREFERENCE_NAME = "skippy_cache";
    private static final int PROGRESS_INTERVAL_MS = 500;
    private static final int NO_CACHE_SIZE = -1;
    private static final int NO_CACHE_SIZE_PERCENTAGE = -1;
    private static final boolean ONE_TRACK_CACHE = true;
    private static final boolean ALL_TRACKS_CACHE = false;
    private static final int MAX_CACHE_SIZE_BYTES = 1000000000;
    private static final int MAX_CACHE_SIZE_PERCENTAGE = 90;
    private static final String NO_CACHE = null;
    private static final byte[] NO_CACHE_ENCRYPTION_KEY = null;


    private final ApplicationProperties applicationProperties;
    private final FeatureFlags featureFlags;
    private final SharedPreferences sharedPreferences;
    private final CryptoOperations cryptoOperations;
    private final String maxSizePercentageKey;

    @Inject
    SkippyFactory(CryptoOperations cryptoOperations, SharedPreferences sharedPreferences, ApplicationProperties applicationProperties,
                  Resources resources, FeatureFlags featureFlags) {
        this.cryptoOperations = cryptoOperations;
        this.sharedPreferences = sharedPreferences;
        this.applicationProperties = applicationProperties;
        this.featureFlags = featureFlags;
        maxSizePercentageKey = resources.getString(R.string.key_stream_cache_size);
    }


    public Skippy create() {
        return new Skippy();
    }

    public Skippy create(Skippy.PlayListener listener) {
        return new Skippy(listener);
    }

    public Skippy.Configuration createConfiguration() {
        if (featureFlags.isEnabled(Feature.SECURE_STREAM_CACHE)) {
            return createConfigurationForSecureCache();
        } else {
            return createConfigurationForSingleTrackCache();
        }
    }

    private Skippy.Configuration createConfigurationForSingleTrackCache() {
        return new Skippy.Configuration(
                PROGRESS_INTERVAL_MS,
                NO_CACHE_SIZE,
                NO_CACHE_SIZE_PERCENTAGE,
                NO_CACHE,
                NO_CACHE_ENCRYPTION_KEY,
                applicationProperties.useVerboseLogging(),
                ONE_TRACK_CACHE
        );
    }

    private Skippy.Configuration createConfigurationForSecureCache() {
        return new Skippy.Configuration(
                PROGRESS_INTERVAL_MS,
                MAX_CACHE_SIZE_BYTES,
                sharedPreferences.getInt(maxSizePercentageKey, MAX_CACHE_SIZE_PERCENTAGE),
                Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY.getAbsolutePath(),
                cryptoOperations.getKeyOrGenerateAndStore(KEY_PREFERENCE_NAME),
                applicationProperties.useVerboseLogging(),
                ALL_TRACKS_CACHE
        );
    }

}
