package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;

@ActiveExperiment
public class PlaylistAndAlbumsPreviewsExperiment {

    private static final String NAME = "albums_bucket_in_collection";

    public static final String VARIANT_CONTROL = "control1";
    public static final String VARIANT_CONTROL_2 = "control2";
    public static final String VARIANT_SEPARATE_PLAYLIST_AND_ALBUMS = "albums_bucket";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_CONTROL_2, VARIANT_SEPARATE_PLAYLIST_AND_ALBUMS));

    private final ExperimentOperations experimentOperations;

    @Inject
    PlaylistAndAlbumsPreviewsExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return VARIANT_SEPARATE_PLAYLIST_AND_ALBUMS.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
