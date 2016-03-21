package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import javax.inject.Inject;
import java.util.Arrays;

public class ShareAsTextButtonExperiment {
    static final String NAME = "android_share_as_text_button";
    static final String VARIATION_TEXT = "text";
    static final String VARIATION_ICON = "icon";
    static final Experiment EXPERIMENT =
            Experiment.create(LISTENING_LAYER, NAME, Arrays.asList(VARIATION_TEXT, VARIATION_ICON));

    private final ExperimentOperations experimentOperations;

    @Inject
    public ShareAsTextButtonExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean showAsText() {
        switch (experimentOperations.getExperimentVariant(NAME)) {
            case VARIATION_TEXT:
                return true;
            case VARIATION_ICON:
            default:
                return false;
        }
    }
}

