package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;

@ActiveExperiment
public class FlipperConfiguration {

    private static final String NAME = "flipper_android";
    static final String VARIANT_CONTROL = "skippy";
    static final String VARIANT_FLIPPER = "flipper";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_FLIPPER));

    private final ExperimentOperations experimentOperations;
    private final FlipperPreloadConfiguration flipperPreloadConfiguration;
    private final FeatureFlags featureFlags;

    @Inject
    FlipperConfiguration(ExperimentOperations experimentOperations, FlipperPreloadConfiguration flipperPreloadConfiguration, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.flipperPreloadConfiguration = flipperPreloadConfiguration;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        switch (getVariant()) {
            case VARIANT_FLIPPER:
                return true;
            case VARIANT_CONTROL:
            default:
                return featureFlags.isEnabled(Flag.FLIPPER) || flipperPreloadConfiguration.isEnabled();
        }
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }

}
