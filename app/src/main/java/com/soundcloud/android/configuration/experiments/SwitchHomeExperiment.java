package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import javax.inject.Inject;
import java.util.Arrays;

public class SwitchHomeExperiment {
    private static final String NAME = "switch_home_on_android";
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_ENABLED = "switch_home_enabled";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL, VARIANT_ENABLED));

    private final ExperimentOperations experimentOperations;

    @Inject
    public SwitchHomeExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return VARIANT_ENABLED.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
