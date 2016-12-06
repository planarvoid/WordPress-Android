package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import javax.inject.Inject;
import java.util.Arrays;

public class SuggestedCreatorsExperiment {
    private static final String NAME = "suggested_creators_rollout2";
    private static final String VARIANT_CONTROL = "no_change";
    private static final String VARIANT_ENABLED = "show_suggested_creators";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL, VARIANT_ENABLED));

    private final ExperimentOperations experimentOperations;

    @Inject
    public SuggestedCreatorsExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return VARIANT_ENABLED.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
