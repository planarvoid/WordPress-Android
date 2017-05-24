package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.collection.SaveCollectionNavigationTarget;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class ChangeLikeToSaveExperiment {
    private static final String NAME = "android_change_like_to_save";
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_CONTROL_PLUS_TOOLTIP = "control_plus_tooltip";
    private static final String VARIANT_SAVE_IN_COPY = "save_in_copy";
    private static final String VARIANT_SAVE_IN_COPY_PLUS_TOOLTIP = "save_in_copy_plus_tooltip";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL,
                                    VARIANT_CONTROL_PLUS_TOOLTIP,
                                    VARIANT_SAVE_IN_COPY,
                                    VARIANT_SAVE_IN_COPY_PLUS_TOOLTIP));

    private final ExperimentOperations experimentOperations;

    @Inject
    ChangeLikeToSaveExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public BaseNavigationTarget navigationTarget() {
        return isEnabled()
               ? new SaveCollectionNavigationTarget()
               : new CollectionNavigationTarget();
    }

    public boolean isEnabled() {
        return isSaveInCopy() || isSaveInCopyPlusTooltip();
    }

    public boolean isTooltipEnabled() {
        return isControlPlusTooltip() || isSaveInCopyPlusTooltip();
    }

    private boolean isControlPlusTooltip() {
        return VARIANT_CONTROL_PLUS_TOOLTIP.equals(getVariant());
    }

    private boolean isSaveInCopy() {
        return VARIANT_SAVE_IN_COPY.equals(getVariant());
    }

    private boolean isSaveInCopyPlusTooltip() {
        return VARIANT_SAVE_IN_COPY_PLUS_TOOLTIP.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
