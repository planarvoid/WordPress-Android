package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaySessionController {

    private static final StateTransition PLAY_QUEUE_COMPLETE_EVENT = new StateTransition(PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE);
    private final Context context;
    private final EventBus eventBus;
    private final PlaybackOperations playbackOperations;
    private final PlayQueueManager playQueueManager;

    private PlayaState currentState = PlayaState.IDLE;
    private TrackUrn currentPlayingUrn;

    private PlaybackProgressEvent currentProgress;

    @Inject
    public PlaySessionController(Context context, EventBus eventBus, PlaybackOperations playbackOperations, PlayQueueManager playQueueManager) {
        this.context = context;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;

    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    public boolean isPlayingTrack(Track track){
        return currentPlayingUrn != null && currentPlayingUrn.equals(track.getUrn());
    }

    public PlaybackProgressEvent getCurrentProgress() {
        return currentProgress;
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            currentState = stateTransition.getNewState();
            currentPlayingUrn = stateTransition.getTrackUrn();

            if (stateTransition.trackEnded()){
                if (playQueueManager.autoNextTrack()){
                    playbackOperations.playCurrent(context);
                } else {
                    eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, PLAY_QUEUE_COMPLETE_EVENT);
                }
            }
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (currentState == PlayaState.PLAYING) {
                playbackOperations.playCurrent(context);
            }
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            currentProgress = progress;
        }
    }
}
