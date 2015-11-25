package com.soundcloud.android.playback;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Locale;

public class StreamCacheConfig {

    @VisibleForTesting
    static final long MIN_SIZE_BYTES = 60  * 1024 * 1024; // 60MB
    static final long MAX_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    static final int STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 1;

    private final TelphonyBasedCountryProvider countryProvider;
    private final File streamCacheDirectory;
    private final IOUtils ioUtils;

    @Inject
    public StreamCacheConfig(TelphonyBasedCountryProvider countryProvider,
                             @Named(StorageModule.STREAM_CACHE_DIRECTORY) File streamCacheDirectory, IOUtils ioUtils) {
        this.countryProvider = countryProvider;
        this.streamCacheDirectory = streamCacheDirectory;
        this.ioUtils = ioUtils;
    }

    public long getStreamCacheSize(){
        final String countryCode = countryProvider.getCountryCode();
        if (Strings.isBlank(countryCode)
                || Locale.US.getCountry().equalsIgnoreCase(countryCode)
                || Locale.GERMANY.getCountry().equalsIgnoreCase(countryCode)
                || Locale.FRANCE.getCountry().equalsIgnoreCase(countryCode)
                || Locale.UK.getCountry().equalsIgnoreCase(countryCode)) {
            return MIN_SIZE_BYTES;
        } else {
            return MAX_SIZE_BYTES;
        }
    }

    public File getStreamCacheDirectory() {
        return streamCacheDirectory;
    }

    public int getStreamCacheMinFreeSpaceAvailablePercentage() {
        return STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE;
    }

    public long getRemainingCacheSpace() {
        final double usableDiskPercentLeft = (((double) streamCacheDirectory.getUsableSpace()) / streamCacheDirectory.getTotalSpace()) * 100;
        final long spaceRemainingUnderPercentCeiling = (long) ((usableDiskPercentLeft - STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE) * streamCacheDirectory.getTotalSpace());
        final long spaceRemainingUnderSizeCeiling = getStreamCacheSize() - ioUtils.dirSize(streamCacheDirectory);
        return Math.max(0, Math.min(spaceRemainingUnderPercentCeiling, spaceRemainingUnderSizeCeiling));
    }
}
