package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import javax.inject.Inject;
import java.util.Arrays;

public class OpusExperiment {
    private static final String NAME = "opus_android";
    private static final String VARIANT_MP3 = "mp3";
    private static final String VARIANT_OPUS = "opus";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_MP3, VARIANT_OPUS));

    private final ExperimentOperations experimentOperations;

    @Inject
    public OpusExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        // Opus is enabled by default and we run the experiment on MP3
        // to compare the performances.
        //
        // The assumption is that Opus is already performing better and the experiment
        // will capture how much better it is.
        return !VARIANT_MP3.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
