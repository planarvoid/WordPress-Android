package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;

@ActiveExperiment
public class FlipperPreloadConfiguration {

    private static final String NAME = "flipper_android2";
    static final String VARIANT_CONTROL = "skippy2";
    static final String VARIANT_FLIPPER = "flipper2";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_FLIPPER));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    FlipperPreloadConfiguration(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        switch (getVariant()) {
            case VARIANT_FLIPPER:
                return true;
            case VARIANT_CONTROL:
            default:
                return featureFlags.isEnabled(Flag.FLIPPER_PRELOAD);
        }
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }

}
