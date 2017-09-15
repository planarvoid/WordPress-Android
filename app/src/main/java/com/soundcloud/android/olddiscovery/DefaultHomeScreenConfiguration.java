package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class DefaultHomeScreenConfiguration {

    private final FeatureOperations featureOperations;
    private final FeatureFlags featureFlags;

    @Inject
    public DefaultHomeScreenConfiguration(FeatureOperations featureOperations, FeatureFlags featureFlags) {
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isDiscoveryHome() {
        return featureFlags.isEnabled(Flag.NEW_HOME) || featureOperations.isNewHomeEnabled();
    }

    public boolean isStreamHome() {
        return !isDiscoveryHome();
    }

    public String screenName() {
        if (isDiscoveryHome()) {
            return Screen.DISCOVER.get();
        } else {
            return Screen.STREAM.get();
        }
    }
}
