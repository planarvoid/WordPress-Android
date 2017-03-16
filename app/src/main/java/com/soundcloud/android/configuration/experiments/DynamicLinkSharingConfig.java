package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class DynamicLinkSharingConfig {
    private static final String NAME = "dynamic_link_sharing_android";
    private static final String CONTROL = "control";
    private static final String DYNAMIC_LINK_SHARING = "dynamic_link_sharing";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(CONTROL, DYNAMIC_LINK_SHARING));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    DynamicLinkSharingConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return isFeatureFlagEnabled() || isExperimentEnabled();
    }

    private boolean isFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.DYNAMIC_LINK_SHARING);
    }

    private boolean isExperimentEnabled() {
        return DYNAMIC_LINK_SHARING.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
