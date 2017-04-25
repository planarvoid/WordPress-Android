package com.soundcloud.android.discovery;


import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.olddiscovery.OldDiscoveryNavigationTarget;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class DiscoveryConfiguration {

    private final FeatureFlags featureFlags;

    @Inject
    DiscoveryConfiguration(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public BaseNavigationTarget navigationTarget() {
        return isEnabled() ? new DiscoveryNavigationTarget() : new OldDiscoveryNavigationTarget();
    }

    private boolean isEnabled() {
        return featureFlags.isEnabled(Flag.DISCOVER_BACKEND);
    }
}
