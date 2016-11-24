package com.soundcloud.android.main;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.search.DiscoveryNavigationTarget;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {

    private final PlaylistDiscoveryConfig playlistDiscoveryConfig;

    @Inject
    public NavigationModelFactory(PlaylistDiscoveryConfig playlistDiscoveryConfig) {
        this.playlistDiscoveryConfig = playlistDiscoveryConfig;
    }

    public NavigationModel build() {
        if (playlistDiscoveryConfig.isEnabled()) {
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
