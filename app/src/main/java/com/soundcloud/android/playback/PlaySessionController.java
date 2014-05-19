package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaySessionController {

    private final Context context;
    private final EventBus eventBus;
    private final PlaybackOperations playbackOperations;

    private PlayaState currentState = PlayaState.IDLE;
    private TrackUrn currentPlayingUrn;

    private PlaybackProgressEvent currentProgress;

    @Inject
    public PlaySessionController(Context context, EventBus eventBus, PlaybackOperations playbackOperations) {
        this.context = context;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    public boolean isPlayingTrack(Track track){
        return currentPlayingUrn != null && track.getUrn().equals(currentPlayingUrn);
    }

    public PlaybackProgressEvent getCurrentProgress() {
        return currentProgress;
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition state) {
            currentState = state.getNewState();
            currentPlayingUrn = state.getTrackUrn();
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
