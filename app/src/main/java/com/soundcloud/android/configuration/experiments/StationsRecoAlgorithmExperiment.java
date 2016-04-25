package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;

public class StationsRecoAlgorithmExperiment {
    private static final String PATTERN = "^reco-radio-.*$";

    private final ExperimentOperations experimentOperations;
    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration.fromPattern(LISTENING_LAYER, PATTERN);

    @Inject
    public StationsRecoAlgorithmExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public Optional<String> getVariantName() {
        final Optional<Layer> layer = experimentOperations.findLayer(CONFIGURATION);
        if (layer.isPresent()) {
            return Optional.of(layer.get().getVariantName());
        } else {
            return Optional.absent();
        }
    }

}
