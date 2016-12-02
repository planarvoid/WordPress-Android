package com.soundcloud.android.playback;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Locale;

public class StreamCacheConfig {

    @VisibleForTesting static final long MIN_SIZE_BYTES = 120 * 1024 * 1024; // 120MB
    @VisibleForTesting static final long MAX_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    @VisibleForTesting static final byte STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 1;

    // These are missing form the Locale class
    private static final String COUNTRY_NEW_ZEALAND = "NZ";
    private static final String COUNTRY_AUSTRALIA = "AU";

    private final TelphonyBasedCountryProvider countryProvider;
    private final IOUtils ioUtils;
    @Nullable private final File streamCacheDirectory;

    StreamCacheConfig(TelphonyBasedCountryProvider countryProvider,
                      IOUtils ioUtils,
                      @Nullable File streamCacheDirectory) {
        this.countryProvider = countryProvider;
        this.streamCacheDirectory = streamCacheDirectory;
        this.ioUtils = ioUtils;
    }

    public long getStreamCacheSize() {
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

    @Nullable
    public File getStreamCacheDirectory() {
        return streamCacheDirectory;
    }

    public byte getStreamCacheMinFreeSpaceAvailablePercentage() {
        return STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE;
    }

    public long getRemainingCacheSpace() {
        if (streamCacheDirectory == null) {
            return 0;
        } else {
            final double usableDiskPercentLeft = (((double) streamCacheDirectory.getUsableSpace()) / streamCacheDirectory
                    .getTotalSpace()) * 100;
            final long spaceRemainingUnderPercentCeiling = (long) ((usableDiskPercentLeft - STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE) * streamCacheDirectory
                    .getTotalSpace());
            final long spaceRemainingUnderSizeCeiling = getStreamCacheSize() - ioUtils.dirSize(streamCacheDirectory);
            return Math.max(0, Math.min(spaceRemainingUnderPercentCeiling, spaceRemainingUnderSizeCeiling));
        }
    }

    public static class SkippyConfig extends StreamCacheConfig {

        @Inject
        SkippyConfig(TelphonyBasedCountryProvider countryProvider,
                     IOUtils ioUtils,
                     @Nullable @Named(StorageModule.STREAM_CACHE_DIRECTORY_SKIPPY) File streamCacheDirectory) {
            super(countryProvider, ioUtils, streamCacheDirectory);
        }
    }

    public static class FlipperConfig extends StreamCacheConfig {

        @Inject
        FlipperConfig(TelphonyBasedCountryProvider countryProvider,
                     IOUtils ioUtils,
                     @Nullable @Named(StorageModule.STREAM_CACHE_DIRECTORY_FLIPPER) File streamCacheDirectory) {
            super(countryProvider, ioUtils, streamCacheDirectory);
        }

        public String getLogFilePath() {
            // TODO: Miloš to update this
            return "";
        }
    }
}
