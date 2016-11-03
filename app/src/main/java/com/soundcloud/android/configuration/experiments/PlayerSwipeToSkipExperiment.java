package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;
import static java.util.Arrays.asList;

import javax.inject.Inject;

public class PlayerSwipeToSkipExperiment {
    private static final String NAME = "player_swipe_to_skip_onboarding";
    // TODO: 11/3/16 check the variants
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_ENABLED = "nudge";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_ENABLED));

    private final ExperimentOperations experimentOperations;

    @Inject
    PlayerSwipeToSkipExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return VARIANT_ENABLED.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
