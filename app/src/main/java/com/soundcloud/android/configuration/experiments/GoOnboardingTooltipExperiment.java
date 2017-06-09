package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class GoOnboardingTooltipExperiment {
    private static final String NAME = "android_subscriber_onboarding_tooltips";
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_CONTROL_2 = "control_2";
    private static final String VARIANT_TOOLTIP_ONBOARDING = "tooltip_onboarding";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL,
                                    VARIANT_CONTROL_2,
                                    VARIANT_TOOLTIP_ONBOARDING));

    private final ExperimentOperations experimentOperations;

    @Inject
    GoOnboardingTooltipExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return VARIANT_TOOLTIP_ONBOARDING.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
