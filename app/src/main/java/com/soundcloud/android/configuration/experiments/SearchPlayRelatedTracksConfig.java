package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;
import java.util.Arrays;

public class SearchPlayRelatedTracksConfig {
    private static final String NAME = "no_visible_play_queue_on_android";
    private static final String CONTROL = "control";
    private static final String PLAY_RELATED_TRACKS = "related_tracks_on_search_results";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(CONTROL, PLAY_RELATED_TRACKS));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    SearchPlayRelatedTracksConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return isFeatureFlagEnabled() || isExperimentEnabled();
    }

    private boolean isFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.SEARCH_PLAY_RELATED_TRACKS);
    }

    private boolean isExperimentEnabled() {
        return PLAY_RELATED_TRACKS.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
