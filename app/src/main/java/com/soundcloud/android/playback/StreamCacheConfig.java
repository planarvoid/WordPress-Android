package com.soundcloud.android.playback;

import com.soundcloud.android.playback.flipper.FlipperCache;
import com.soundcloud.android.playback.skippy.SkippyCache;
import com.soundcloud.android.utils.CountryProvider;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import java.io.File;
import java.util.Locale;

public class StreamCacheConfig<Key> implements PlayerCache<Key> {

    @VisibleForTesting static final long MIN_SIZE_BYTES = 120 * 1024 * 1024; // 120MB
    @VisibleForTesting static final long MAX_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    @VisibleForTesting static final byte STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 1;

    // These are missing form the Locale class
    private static final String COUNTRY_NEW_ZEALAND = "NZ";
    private static final String COUNTRY_AUSTRALIA = "AU";

    private final Context context;
    private final CountryProvider countryProvider;
    private final IOUtils ioUtils;
    private final Key cacheKey;
    @Nullable private final File cacheDir;

    StreamCacheConfig(Context context, CountryProvider countryProvider, IOUtils ioUtils, Key cacheKey, @Nullable File cacheDirectory) {
        this.context = context;
        this.countryProvider = countryProvider;
        this.cacheKey = cacheKey;
        this.cacheDir = cacheDirectory;
        this.ioUtils = ioUtils;
    }

    @NotNull
    @Override
    public Key key() {
        return cacheKey;
    }

    @Nullable
    @Override
    public File directory() {
        if (cacheDir != null && !cacheDir.exists()) {
            IOUtils.createCacheDirs(context, cacheDir);
        }
        return cacheDir;
    }

    @Override
    public long size() {
        final String countryCode = countryProvider.getCountryCode();
        if (Strings.isBlank(countryCode)
                || Locale.US.getCountry().equalsIgnoreCase(countryCode)
                || Locale.GERMANY.getCountry().equalsIgnoreCase(countryCode)
                || Locale.FRANCE.getCountry().equalsIgnoreCase(countryCode)
                || Locale.UK.getCountry().equalsIgnoreCase(countryCode)
                || COUNTRY_NEW_ZEALAND.equalsIgnoreCase(countryCode)
                || COUNTRY_AUSTRALIA.equalsIgnoreCase(countryCode)) {
            return MIN_SIZE_BYTES;
        } else {
            return MAX_SIZE_BYTES;
        }
    }

    @Override
    public byte minFreeSpaceAvailablePercentage() {
        return STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE;
    }

    @Override
    public long remainingSpace() {
        if (cacheDir == null) {
            return 0;
        } else {
            final double usableDiskPercentLeft = (((double) cacheDir.getUsableSpace()) / cacheDir.getTotalSpace()) * 100;
            final long spaceRemainingUnderPercentCeiling = (long) ((usableDiskPercentLeft - STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE) * cacheDir.getTotalSpace());
            final long spaceRemainingUnderSizeCeiling = size() - ioUtils.dirSize(cacheDir);
            return Math.max(0, Math.min(spaceRemainingUnderPercentCeiling, spaceRemainingUnderSizeCeiling));
        }
    }

    static class SkippyConfig extends StreamCacheConfig<byte[]> implements SkippyCache {

        SkippyConfig(Context context,
                     CountryProvider countryProvider,
                     IOUtils ioUtils,
                     byte[] cacheKey,
                     File streamCacheDirectory) {
            super(context, countryProvider, ioUtils, cacheKey, streamCacheDirectory);
        }
    }

    static class FlipperConfig extends StreamCacheConfig<String> implements FlipperCache {

        FlipperConfig(Context context,
                      CountryProvider countryProvider,
                      IOUtils ioUtils,
                      String cacheKey,
                      File streamCacheDirectory) {
            super(context, countryProvider, ioUtils, cacheKey, streamCacheDirectory);
        }
    }
}
