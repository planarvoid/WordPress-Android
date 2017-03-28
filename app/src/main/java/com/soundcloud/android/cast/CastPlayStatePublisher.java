package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.api.CastProtocol.TAG;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastPlayStatePublisher {

    private final PlaySessionStateProvider playSessionStateProvider;
    private final EventBus eventBus;
    private final CastConnectionHelper castConnectionHelper;

    @Inject
    public CastPlayStatePublisher(PlaySessionStateProvider playSessionStateProvider,
                                  EventBus eventBus,
                                  CastConnectionHelper castConnectionHelper) {
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
        this.castConnectionHelper = castConnectionHelper;
    }

    public void publish(PlaybackStateTransition stateTransition, PlaybackItem currentPlaybackItem) {
        if (shouldIgnoreStateTransition(stateTransition)) {
            Log.d(TAG, this.getClass().getSimpleName() + " ignored " + stateTransition.getNewState() + " as Casting is off");
            return;
        }

        PlayStateEvent playStateEvent = playSessionStateProvider.onPlayStateTransition(stateTransition, currentPlaybackItem.getDuration());
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
    }

    private boolean shouldIgnoreStateTransition(PlaybackStateTransition stateTransition) {
        // we still want to let the disconnection transition slip
        return !stateTransition.isCastDisconnection() && !castConnectionHelper.isCasting();
    }
}
