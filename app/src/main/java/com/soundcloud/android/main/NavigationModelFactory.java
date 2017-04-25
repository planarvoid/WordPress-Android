package com.soundcloud.android.main;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.discovery.DiscoveryConfiguration;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.olddiscovery.OldDiscoveryNavigationTarget;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {


    private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    private DiscoveryConfiguration discoveryConfiguration;

    @Inject
    public NavigationModelFactory(DefaultHomeScreenConfiguration defaultHomeScreenConfiguration, DiscoveryConfiguration discoveryConfiguration) {
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
        this.discoveryConfiguration = discoveryConfiguration;
    }

    public NavigationModel build() {
        if (defaultHomeScreenConfiguration.isDiscoveryHome()) {
            return new NavigationModel(
                    discoveryConfiguration.navigationTarget(),
                    new StreamNavigationTarget(false),
                    new CollectionNavigationTarget(),
                    new MoreNavigationTarget());
        }

        return new NavigationModel(
                new StreamNavigationTarget(true),
                new OldDiscoveryNavigationTarget(),
                new CollectionNavigationTarget(),
                new MoreNavigationTarget());

    }
}
