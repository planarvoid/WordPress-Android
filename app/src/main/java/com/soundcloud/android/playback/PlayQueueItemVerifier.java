package com.soundcloud.android.playback;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import javax.inject.Inject;

public class PlayQueueItemVerifier {

    private final NetworkConnectionHelper networkConnectionHelper;
    private final TrackOfflineStateProvider offlineStateProvider;

    @Inject
    public PlayQueueItemVerifier(NetworkConnectionHelper networkConnectionHelper,
                                 TrackOfflineStateProvider offlineStateProvider) {
        this.networkConnectionHelper = networkConnectionHelper;
        this.offlineStateProvider = offlineStateProvider;
    }

    public boolean isItemPlayable(PlayQueueItem playQueueItem) {
        return isNotBlockedTrackOrAd(playQueueItem) &&
                (networkConnectionHelper.isNetworkConnected() || isOfflineAvailable(playQueueItem));
    }


    private boolean isOfflineAvailable(PlayQueueItem playQueueItem) {
        return offlineStateProvider.getOfflineState(playQueueItem.getUrn()) == OfflineState.DOWNLOADED;
    }

    private boolean isNotBlockedTrackOrAd(PlayQueueItem playQueueItem) {
        return playQueueItem.isAd() || (playQueueItem.isTrack() && !((TrackQueueItem) playQueueItem).isBlocked());
    }

}
