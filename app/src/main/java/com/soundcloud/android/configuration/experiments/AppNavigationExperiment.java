package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class AppNavigationExperiment {

    private final FeatureFlags featureFlags;

    @Inject
    AppNavigationExperiment(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public boolean isBottomNavigationEnabled() {
        return featureFlags.isEnabled(Flag.BOTTOM_NAVIGATION);
    }
}
