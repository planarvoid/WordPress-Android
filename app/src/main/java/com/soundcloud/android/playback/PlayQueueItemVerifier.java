package com.soundcloud.android.playback;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.utils.ConnectionHelper;

import javax.inject.Inject;

public class PlayQueueItemVerifier {

    private final ConnectionHelper connectionHelper;
    private final TrackOfflineStateProvider offlineStateProvider;

    @Inject
    public PlayQueueItemVerifier(ConnectionHelper connectionHelper,
                                 TrackOfflineStateProvider offlineStateProvider) {
        this.connectionHelper = connectionHelper;
        this.offlineStateProvider = offlineStateProvider;
    }

    public boolean isItemPlayable(PlayQueueItem playQueueItem) {
        return isNotBlockedTrackOrAd(playQueueItem) &&
                (connectionHelper.isNetworkConnected() || isOfflineAvailable(playQueueItem));
    }


    private boolean isOfflineAvailable(PlayQueueItem playQueueItem) {
        return offlineStateProvider.getOfflineState(playQueueItem.getUrn()) == OfflineState.DOWNLOADED;
    }

    private boolean isNotBlockedTrackOrAd(PlayQueueItem playQueueItem) {
        return playQueueItem.isAd() || (playQueueItem.isTrack() && !((TrackQueueItem) playQueueItem).isBlocked());
    }

}
