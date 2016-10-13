package com.soundcloud.android.playback;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Locale;

public class StreamCacheConfig {

    @VisibleForTesting
    static final long MIN_SIZE_BYTES = 120 * 1024 * 1024; // 120MB
    static final long MAX_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    static final int STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 1;

    // These are missing form the Locale class
    private static final String COUNTRY_NEW_ZEALAND = "NZ";
    private static final String COUNTRY_AUSTRALIA = "AU";

    private final TelphonyBasedCountryProvider countryProvider;
    @Nullable
    private final File streamCacheDirectory;
    private final IOUtils ioUtils;

    @Inject
    public StreamCacheConfig(TelphonyBasedCountryProvider countryProvider,
                             @Nullable @Named(StorageModule.STREAM_CACHE_DIRECTORY) File streamCacheDirectory, IOUtils ioUtils) {
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

    public int getStreamCacheMinFreeSpaceAvailablePercentage() {
        return STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE;
    }

    public long getRemainingCacheSpace() {
        if (streamCacheDirectory == null) {
            return 0;
        } else {
            final double usableDiskPercentLeft = (((double) streamCacheDirectory.getUsableSpace()) / streamCacheDirectory.getTotalSpace()) * 100;
            final long spaceRemainingUnderPercentCeiling = (long) ((usableDiskPercentLeft - STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE) * streamCacheDirectory
                    .getTotalSpace());
            final long spaceRemainingUnderSizeCeiling = getStreamCacheSize() - ioUtils.dirSize(streamCacheDirectory);
            return Math.max(0, Math.min(spaceRemainingUnderPercentCeiling, spaceRemainingUnderSizeCeiling));
        }
    }
}
