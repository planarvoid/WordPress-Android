package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.AdToastViewController;
import com.soundcloud.android.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastSessionReconnector implements CastConnectionHelper.CastConnectionListener {

    private final PlaybackOperations playbackOperations;
    private final PlayQueueManager playQueueManager;
    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;
    private final AdToastViewController adToastViewController;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    public CastSessionReconnector(PlaybackOperations playbackOperations, PlayQueueManager playQueueManager,
                                  CastConnectionHelper castConnectionHelper, EventBus eventBus, AdToastViewController adToastViewController, PlaySessionStateProvider playSessionStateProvider) {
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;
        this.adToastViewController = adToastViewController;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void startListening() {
        castConnectionHelper.addConnectionListener(this);
    }

    @Override
    public void onConnectedToReceiverApp() {
        if (playSessionStateProvider.isPlaying()){
            playbackOperations.stopService();
            final PlaybackProgress lastProgressByUrn = playSessionStateProvider.getLastProgressByUrn(playQueueManager.getCurrentTrackUrn());
            playbackOperations.playCurrent(lastProgressByUrn.getPosition());
        }
    }

    @Override
    public void onMetaDataUpdated(Urn currentUrn) {
        if (playQueueManager.isQueueEmpty()) {
            playbackOperations.playTrackWithRecommendations(currentUrn, PlaySessionSource.EMPTY)
                    .subscribe(new ShowPlayerSubscriber(eventBus, adToastViewController));
        }
    }
}
