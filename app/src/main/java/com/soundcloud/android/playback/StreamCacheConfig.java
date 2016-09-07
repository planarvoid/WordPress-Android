package com.soundcloud.android.playback;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.annotations.VisibleForTesting;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

public class StreamCacheConfig {

    @VisibleForTesting
    static final long SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    static final int STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE = 1;

    @Nullable
    private final File streamCacheDirectory;
    private final IOUtils ioUtils;

    @Inject
    public StreamCacheConfig(@Named(StorageModule.STREAM_CACHE_DIRECTORY) File streamCacheDirectory, IOUtils ioUtils) {
        this.streamCacheDirectory = streamCacheDirectory;
        this.ioUtils = ioUtils;
    }

    public long getStreamCacheSize() {
        return SIZE_BYTES;
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
