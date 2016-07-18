package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;
import java.util.Arrays;

public class PlayHistoryExperiment {
    private static final String NAME = "play_history";
    private static final String VARIANT_COLLECTION_ABOVE = "collection_above";
    private static final String VARIANT_COLLECTION_BELOW = "collection_below";
    private static final String VARIANT_SEARCH = "search";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_COLLECTION_ABOVE, VARIANT_COLLECTION_BELOW, VARIANT_SEARCH));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    public PlayHistoryExperiment(ExperimentOperations experimentOperations,
                                 FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        switch (getVariant()) {
            case VARIANT_COLLECTION_ABOVE:
            case VARIANT_COLLECTION_BELOW:
            case VARIANT_SEARCH:
                return true;
            default:
                return featureFlags.isEnabled(Flag.LOCAL_PLAY_HISTORY);
        }
    }

    public boolean showOnlyOnSearch() {
        return VARIANT_SEARCH.equals(getVariant());
    }

    public boolean showBelowListeningHistory() {
        return VARIANT_COLLECTION_BELOW.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
