package com.soundcloud.android.discovery;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;

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
}
