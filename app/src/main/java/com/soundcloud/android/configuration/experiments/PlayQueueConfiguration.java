package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;
import java.util.Arrays;

public class PlayQueueConfiguration {

    private static final String NAME = "play_queue";
    static final String VARIANT_CONTROL = "no_play_queue";
    static final String VARIANT_PLAY_QUEUE = "play_queue";

    static final ExperimentConfiguration CONFIGURATION =
            ExperimentConfiguration.fromName(LISTENING_LAYER, NAME, Arrays.asList(VARIANT_CONTROL, VARIANT_PLAY_QUEUE));

    private final ExperimentOperations experimentOperations;
    private final CastConnectionHelper castConnectionHelper;
    private final FeatureFlags featureFlags;

    @Inject
    public PlayQueueConfiguration(ExperimentOperations experimentOperations,
                                  CastConnectionHelper castConnectionHelper,
                                  FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.castConnectionHelper = castConnectionHelper;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return !castConnectionHelper.isCasting() &&
                (featureFlags.isEnabled(Flag.PLAY_QUEUE) || VARIANT_PLAY_QUEUE.equals(getVariant()));
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }


}
