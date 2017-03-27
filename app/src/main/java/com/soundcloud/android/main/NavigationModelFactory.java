package com.soundcloud.android.main;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.discovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.search.DiscoveryNavigationTarget;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {


    private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    @Inject
    public NavigationModelFactory(DefaultHomeScreenConfiguration defaultHomeScreenConfiguration) {
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
    }

    public NavigationModel build() {
        if (defaultHomeScreenConfiguration.isDiscoveryHome()) {
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
