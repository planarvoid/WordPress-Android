package com.soundcloud.android.offline;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.java.collections.PropertySet;

import javax.inject.Inject;

public class OfflinePlaybackOperations {

    private final FeatureOperations featureOperations;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations) {
        this.featureOperations = featureOperations;
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE) == OfflineState.DOWNLOADED;
    }
}
