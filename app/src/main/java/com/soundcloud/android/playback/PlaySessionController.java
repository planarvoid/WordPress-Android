package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
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

    @Inject
    public PlaySessionController(Context context, EventBus eventBus, PlaybackOperations playbackOperations) {
        this.context = context;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition state) {
            currentState = state.getNewState();
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
}
