package com.soundcloud.android.main;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.configuration.experiments.SwitchHomeExperiment;
import com.soundcloud.android.more.MoreNavigationTarget;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.DiscoveryNavigationTarget;
import com.soundcloud.android.stream.StreamNavigationTarget;

import javax.inject.Inject;

public class NavigationModelFactory {

    private final FeatureFlags featureFlags;
    private final SwitchHomeExperiment switchHomeExperiment;

    @Inject
    public NavigationModelFactory(FeatureFlags featureFlags, SwitchHomeExperiment switchHomeExperiment) {
        this.featureFlags = featureFlags;
        this.switchHomeExperiment = switchHomeExperiment;
    }

    public NavigationModel build() {
        if (featureFlags.isEnabled(Flag.NEW_HOME) || switchHomeExperiment.isEnabled()) {
            return new NavigationModel(
                    new DiscoveryNavigationTarget(true),
                    new StreamNavigationTarget(false),
                    new CollectionNavigationTarget(),
                    new MoreNavigationTarget());
        }

        return new NavigationModel(
                new StreamNavigationTarget(true),
                new DiscoveryNavigationTarget(false),
                new CollectionNavigationTarget(),
                new MoreNavigationTarget());

    }
}
