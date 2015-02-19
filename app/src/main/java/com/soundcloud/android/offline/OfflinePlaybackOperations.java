package com.soundcloud.android.offline;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

public class OfflinePlaybackOperations {

    private final FeatureOperations featureOperations;
    private final NetworkConnectionHelper connectionHelper;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations, NetworkConnectionHelper connectionHelper) {
        this.featureOperations = featureOperations;
        this.connectionHelper = connectionHelper;
    }

    public boolean shouldCreateOfflinePlayQueue() {
        return featureOperations.isOfflineContentEnabled() && !connectionHelper.isNetworkConnected();
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElseNull(TrackProperty.OFFLINE_DOWNLOADED_AT) != null
                && track.getOrElseNull(TrackProperty.OFFLINE_REMOVED_AT) == null;
    }

}
