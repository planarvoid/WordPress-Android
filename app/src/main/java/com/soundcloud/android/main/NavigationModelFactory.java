package com.soundcloud.android.main;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.search.DiscoveryNavigationTarget;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {

    private final PlaylistDiscoveryConfig playlistDiscoveryConfig;

    private final FeatureOperations featureOperations;

    @Inject
    public NavigationModelFactory(PlaylistDiscoveryConfig playlistDiscoveryConfig, FeatureOperations featureOperations) {
        this.playlistDiscoveryConfig = playlistDiscoveryConfig;
        this.featureOperations = featureOperations;
    }

    public NavigationModel build() {
        if (playlistDiscoveryConfig.isEnabled() || featureOperations.isNewHomeEnabled()) {
            return new NavigationModel(
                    new DiscoveryNavigationTarget(),
                    new StreamNavigationTarget(false),
                    new CollectionNavigationTarget(),
                    new MoreNavigationTarget());
        }

        return new NavigationModel(
                new StreamNavigationTarget(true),
                new DiscoveryNavigationTarget(),
                new CollectionNavigationTarget(),
                new MoreNavigationTarget());

    }
}
