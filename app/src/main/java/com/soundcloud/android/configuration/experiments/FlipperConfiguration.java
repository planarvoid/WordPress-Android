package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class FlipperConfiguration {

    private static final String NAME = "flipper_android";
    static final String VARIANT_CONTROL = "skippy";
    static final String VARIANT_FLIPPER = "flipper";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_FLIPPER));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    FlipperConfiguration(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        switch (getVariant()) {
            case VARIANT_FLIPPER:
                return true;
            case VARIANT_CONTROL:
            default:
                return featureFlags.isEnabled(Flag.FLIPPER);
        }
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }

}
