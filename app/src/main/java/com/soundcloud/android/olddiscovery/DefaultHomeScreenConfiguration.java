package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.main.Screen;

import javax.inject.Inject;

public class DefaultHomeScreenConfiguration {

    private final FeatureOperations featureOperations;

    @Inject
    public DefaultHomeScreenConfiguration(FeatureOperations featureOperations) {
        this.featureOperations = featureOperations;
    }

    public boolean isDiscoveryHome() {
        return featureOperations.isNewHomeEnabled();
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
