package com.soundcloud.android.offline;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.List;

public class OfflinePlaybackOperations {

    private final FeatureOperations featureOperations;
    private final TrackDownloadsStorage trackDownloadsStorage;
    private final TrackOfflineStateProvider trackOfflineStateProvider;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations, TrackDownloadsStorage trackDownloadsStorage, TrackOfflineStateProvider trackOfflineStateProvider) {
        this.featureOperations = featureOperations;
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.trackOfflineStateProvider = trackOfflineStateProvider;
    }

    public boolean shouldPlayOffline(Urn track) {
        return featureOperations.isOfflineContentEnabled()
                && trackOfflineStateProvider.getOfflineState(track) == OfflineState.DOWNLOADED;
    }

    List<Urn> findOfflineAvailableTracks(List<Urn> urns) {
        return trackDownloadsStorage.onlyOfflineTracks(urns);
    }
}
