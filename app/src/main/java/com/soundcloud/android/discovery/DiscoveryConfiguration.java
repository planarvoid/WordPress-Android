package com.soundcloud.android.discovery;

import com.soundcloud.android.configuration.experiments.StaticDiscoverContentExperiment;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.olddiscovery.OldDiscoveryNavigationTarget;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class DiscoveryConfiguration {

    private final StaticDiscoverContentExperiment staticDiscoverContentExperiment;
    private final FeatureFlags featureFlags;

    @Inject
    DiscoveryConfiguration(StaticDiscoverContentExperiment staticDiscoverContentExperiment, FeatureFlags featureFlags) {
        this.staticDiscoverContentExperiment = staticDiscoverContentExperiment;
        this.featureFlags = featureFlags;
    }

    public BaseNavigationTarget navigationTarget() {
        return shouldShowDiscoverBackendContent() ? new DiscoveryNavigationTarget(featureFlags) : new OldDiscoveryNavigationTarget();
    }

    public boolean shouldShowDiscoverBackendContent() {
        return isDiscoverBackendFeatureFlagEnabled() && !staticDiscoverContentExperiment.isEnabled();
    }

    private boolean isDiscoverBackendFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.DISCOVER_BACKEND);
    }
}
