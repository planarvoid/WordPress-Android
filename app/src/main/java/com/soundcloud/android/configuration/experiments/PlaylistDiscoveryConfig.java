package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;
import java.util.Arrays;

public class PlaylistDiscoveryConfig {
    private static final String NAME = "android_repackaged_playlist_discovery";
    static final String VARIANT_CONTROL = "repackaged_pd_control";
    static final String VARIANT_PLAYLIST_DISCOVERY_FIRST = "repackaged_pd_playlists_first";
    static final String VARIANT_SUGGESTED_STATIONS_FIRST = "repackaged_pd_stations_first";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL,
                                    VARIANT_PLAYLIST_DISCOVERY_FIRST,
                                    VARIANT_SUGGESTED_STATIONS_FIRST));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    public PlaylistDiscoveryConfig(ExperimentOperations experimentOperations,
                                   FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return featureFlags.isEnabled(Flag.NEW_HOME) || isPlaylistDiscoveryFirst() || isSuggestedStationsFirst();
    }

    public boolean isPlaylistDiscoveryFirst() {
        return VARIANT_PLAYLIST_DISCOVERY_FIRST.equals(getVariant());
    }

    private boolean isSuggestedStationsFirst() {
        return VARIANT_SUGGESTED_STATIONS_FIRST.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
