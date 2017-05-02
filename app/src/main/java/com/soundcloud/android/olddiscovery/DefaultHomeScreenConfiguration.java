package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.main.Screen;

import javax.inject.Inject;

public class DefaultHomeScreenConfiguration {

    private final PlaylistDiscoveryConfig playlistDiscoveryConfig;
    private final FeatureOperations featureOperations;

    @Inject
    public DefaultHomeScreenConfiguration(PlaylistDiscoveryConfig playlistDiscoveryConfig,
                                          FeatureOperations featureOperations) {
        this.playlistDiscoveryConfig = playlistDiscoveryConfig;
        this.featureOperations = featureOperations;
    }

    public boolean isDiscoveryHome() {
        return playlistDiscoveryConfig.isEnabled() || featureOperations.isNewHomeEnabled();
    }

    public boolean isStreamHome() {
        return !isDiscoveryHome();
    }

    public String screenName() {
        if (isDiscoveryHome()) {
            return Screen.SEARCH_MAIN.get();
        } else {
            return Screen.STREAM.get();
        }
    }
}
