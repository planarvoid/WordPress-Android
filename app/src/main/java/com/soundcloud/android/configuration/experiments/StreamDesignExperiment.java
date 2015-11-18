package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class StreamDesignExperiment {
    public static final String NAME = "android_stream_design";

    private static final String VARIATION_LIST = "old_list_stream";
    private static final String VARIATION_CARDS = "new_card_stream";

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    public StreamDesignExperiment(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isCardDesign() {
        switch (experimentOperations.getExperimentVariant(NAME)) {
            case VARIATION_CARDS:
                return true;
            case VARIATION_LIST:
                return false;
            default:
                return featureFlags.isEnabled(Flag.NEW_STREAM);
        }
    }
}
