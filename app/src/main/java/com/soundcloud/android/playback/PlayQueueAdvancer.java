package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player.StateTransition;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueAdvancer {

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final NetworkConnectionHelper connectionHelper;
    private final PlaySessionController playSessionController;
    private final AdsController adsController;

    private final Func1<StateTransition, Boolean> shouldAdvanceTracks = new Func1<StateTransition, Boolean>() {
        @Override
        public Boolean call(StateTransition stateTransition) {
            return playQueueManager.isCurrentTrack(stateTransition.getUrn())
                    && stateTransition.isPlayerIdle()
                    && !stateTransition.isPlayQueueComplete()
                    && (stateTransition.playbackEnded() || unrecoverableErrorDuringAutoplay(stateTransition));
        }
    };

    private final Action1<Object> reconfigureUpcomingAd = new Action1<Object>() {
        @Override
        public void call(Object ignored) {
            adsController.reconfigureAdForNextTrack();
            adsController.publishAdDeliveryEventIfUpcoming();
        }
    };

    @Inject
    public PlayQueueAdvancer(EventBus eventBus,
                             PlayQueueManager playQueueManager,
                             NetworkConnectionHelper connectionHelper,
                             PlaySessionController playSessionController,
                             AdsController adsController) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.connectionHelper = connectionHelper;
        this.playSessionController = playSessionController;
        this.adsController = adsController;
    }

    public void subscribe(){
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(shouldAdvanceTracks)
                .doOnNext(reconfigureUpcomingAd)
                .subscribe(new AdvanceTrackSubscriber());

    }

    private boolean unrecoverableErrorDuringAutoplay(StateTransition stateTransition) {
        final TrackSourceInfo currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        return stateTransition.wasError() && !stateTransition.wasGeneralFailure() &&
                currentTrackSourceInfo != null && !currentTrackSourceInfo.getIsUserTriggered()
                && connectionHelper.isNetworkConnected();
    }

    private class AdvanceTrackSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            if (!playQueueManager.autoMoveToNextPlayableItem()) {
                eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createPlayQueueCompleteEvent(stateTransition.getUrn()));
            } else if (!stateTransition.playSessionIsActive()) {
                playSessionController.playCurrent();
            }
        }
    }

    private StateTransition createPlayQueueCompleteEvent(Urn trackUrn) {
        return new StateTransition(Player.PlayerState.IDLE, Player.Reason.PLAY_QUEUE_COMPLETE, trackUrn);
    }

}
