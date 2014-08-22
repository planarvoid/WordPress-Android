package com.soundcloud.android.playback.service.skippy;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.skippy.Skippy;

import javax.inject.Inject;

public class SkippyFactory {

    private final ApplicationProperties applicationProperties;

    @Inject
    SkippyFactory(ApplicationProperties applicationProperties) {
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
                500, // progress interval
                -1, // maxCacheSizeInBytes
                -1, // maxCacheSizeInPercentage
                null, // cachePath
                null, // cacheKey
                applicationProperties.useVerboseLogging(), // debug logging
                true // use old caching (only the playing track
        );
    }
}
