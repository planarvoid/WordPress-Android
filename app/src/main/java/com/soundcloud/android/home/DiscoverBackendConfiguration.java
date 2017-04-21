package com.soundcloud.android.home;


import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

class DiscoverBackendConfiguration {

    private final FeatureFlags featureFlags;

    @Inject
    DiscoverBackendConfiguration(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    boolean isEnabled() {
        return featureFlags.isEnabled(Flag.DISCOVER_BACKEND);
    }
}
