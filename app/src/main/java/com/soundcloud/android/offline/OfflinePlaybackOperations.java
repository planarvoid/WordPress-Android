package com.soundcloud.android.offline;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import javax.inject.Inject;
import java.util.List;

public class OfflinePlaybackOperations {

    private final FeatureOperations featureOperations;
    private final TrackDownloadsStorage trackDownloadsStorage;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations, TrackDownloadsStorage trackDownloadsStorage) {
        this.featureOperations = featureOperations;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE) == OfflineState.DOWNLOADED;
    }

    public List<Urn> findOfflineAvailableTracks(List<Urn> urns) {
        return trackDownloadsStorage.onlyOfflineTracks(urns);
    }
}
