package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class NewForYouConfig {
    private static final String NAME = "new_for_you_android";
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_NEW_FOR_YOU_TOP = "new_for_you_top";
    private static final String VARIANT_NEW_FOR_YOU_SECOND = "new_for_you_under_recos";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL,
                                    VARIANT_NEW_FOR_YOU_TOP,
                                    VARIANT_NEW_FOR_YOU_SECOND));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    NewForYouConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isTopPositionEnabled() {
        return isTopPositionFeatureFlagEnabled() || isTopPositionExperimentEnabled();
    }

    private boolean isTopPositionFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.NEW_FOR_YOU_FIRST);
    }

    private boolean isTopPositionExperimentEnabled() {
        return VARIANT_NEW_FOR_YOU_TOP.equals(getVariant());
    }

    public boolean isSecondPositionEnabled() {
        return isSecondPositionFeatureFlagEnabled() || isSecondPositionExperimentEnabled();
    }

    private boolean isSecondPositionFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.NEW_FOR_YOU_SECOND);
    }

    private boolean isSecondPositionExperimentEnabled() {
        return VARIANT_NEW_FOR_YOU_SECOND.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
