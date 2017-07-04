package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class FlipperExperiment {
    private static final String NAME = "flipper-android-3";
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_FLIPPER = "flipper";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL, VARIANT_FLIPPER));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    FlipperExperiment(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return !isControlVariant() && (isFlipperVariant() || featureFlags.isEnabled(Flag.FLIPPER));
    }

    private boolean isControlVariant() {
        return VARIANT_CONTROL.equals(getVariant());
    }

    private boolean isFlipperVariant() {
        return VARIANT_FLIPPER.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
