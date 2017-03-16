package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class SuggestedStationsExperiment {
    private static final String NAME = "suggested_stations";
    private static final String VARIANT_CONTROL = "control";
    private static final String VARIANT_TRACK_STATIONS = "track_stations";
    private static final String VARIANT_SUGGESTED_ARTIST_STATIONS = "suggested_artist_stations";

    private final ExperimentOperations experimentOperations;
    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, Arrays.asList(VARIANT_CONTROL, VARIANT_TRACK_STATIONS, VARIANT_SUGGESTED_ARTIST_STATIONS));

    @Inject
    public SuggestedStationsExperiment(ExperimentOperations experimentOperations) {
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
