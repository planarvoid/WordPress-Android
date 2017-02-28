package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;
import java.util.Arrays;

public class OtherPlaylistsByUserConfig {
    static final String NAME = "other_playlists_by_user";
    static final String CONTROL = "control";
    static final String HIDE_OTHER_PLAYLISTS = "hide_other_playlists";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(CONTROL, HIDE_OTHER_PLAYLISTS));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    OtherPlaylistsByUserConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return isNewPlaylistScreenEnabled() && isFeatureFlagEnabled() && isExperimentEnabled();
    }

    private boolean isNewPlaylistScreenEnabled() {
        return featureFlags.isEnabled(Flag.NEW_PLAYLIST_SCREEN);
    }

    private boolean isFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.OTHER_PLAYLISTS_BY_CREATOR);
    }

    private boolean isExperimentEnabled() {
        return !HIDE_OTHER_PLAYLISTS.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
