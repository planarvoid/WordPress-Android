package com.soundcloud.android.main;

import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.discovery.DiscoveryNavigationTarget;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.SearchNavigationTarget;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {

    private final DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    private final FeatureFlags featureFlags;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Inject
    NavigationModelFactory(DefaultHomeScreenConfiguration defaultHomeScreenConfiguration,
                           FeatureFlags featureFlags,
                           ChangeLikeToSaveExperiment changeLikeToSaveExperiment) {
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
        this.featureFlags = featureFlags;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
    }

    public NavigationModel build() {
        if (defaultHomeScreenConfiguration.isDiscoveryHome()) {
            if (featureFlags.isEnabled(Flag.SEPARATE_SEARCH)) {
                return new NavigationModel(
                        new DiscoveryNavigationTarget(featureFlags),
                        new StreamNavigationTarget(featureFlags, false),
                        new SearchNavigationTarget(),
                        changeLikeToSaveExperiment.navigationTarget(),
                        new MoreNavigationTarget());
            }
            return new NavigationModel(
                    new DiscoveryNavigationTarget(featureFlags),
                    new StreamNavigationTarget(featureFlags, false),
                    changeLikeToSaveExperiment.navigationTarget(),
                    new MoreNavigationTarget());
        }
        return new NavigationModel(
                new StreamNavigationTarget(featureFlags, true),
                new DiscoveryNavigationTarget(featureFlags),
                changeLikeToSaveExperiment.navigationTarget(),
                new MoreNavigationTarget());
    }
}
