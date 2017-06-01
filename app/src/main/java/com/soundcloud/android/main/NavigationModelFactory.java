package com.soundcloud.android.main;

import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.discovery.DiscoveryConfiguration;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {

    private final DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    private final DiscoveryConfiguration discoveryConfiguration;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Inject
    NavigationModelFactory(DefaultHomeScreenConfiguration defaultHomeScreenConfiguration,
                           DiscoveryConfiguration discoveryConfiguration,
                           ChangeLikeToSaveExperiment changeLikeToSaveExperiment) {
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
        this.discoveryConfiguration = discoveryConfiguration;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
    }

    public NavigationModel build() {
        if (defaultHomeScreenConfiguration.isDiscoveryHome()) {
            return new NavigationModel(
                    discoveryConfiguration.navigationTarget(),
                    new StreamNavigationTarget(false),
                    changeLikeToSaveExperiment.navigationTarget(),
                    new MoreNavigationTarget());
        }

        return new NavigationModel(
                new StreamNavigationTarget(true),
                discoveryConfiguration.navigationTarget(),
                changeLikeToSaveExperiment.navigationTarget(),
                new MoreNavigationTarget());

    }
}
