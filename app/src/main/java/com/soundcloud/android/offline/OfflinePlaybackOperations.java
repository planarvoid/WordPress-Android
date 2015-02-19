package com.soundcloud.android.offline;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import javax.inject.Inject;

public class OfflinePlaybackOperations {

    private final FeatureOperations featureOperations;
    private final NetworkConnectionHelper connectionHelper;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations, NetworkConnectionHelper connectionHelper) {
        this.featureOperations = featureOperations;
        this.connectionHelper = connectionHelper;
    }

    public boolean isOfflinePlaybackMode() {
        return featureOperations.isOfflineContentEnabled() && !connectionHelper.isNetworkConnected();
    }

}
