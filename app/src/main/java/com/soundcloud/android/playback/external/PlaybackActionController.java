package com.soundcloud.android.playback.external;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;

import android.content.Context;

import javax.inject.Inject;

public class PlaybackActionController {

    private final PlaybackOperations playbackOperations;
    private final PlaySessionController playSessionController;
    private final EventBus eventBus;

    @Inject
    public PlaybackActionController(PlaybackOperations playbackOperations, PlaySessionController playSessionController, EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.playSessionController = playSessionController;
        this.eventBus = eventBus;
    }

    public void handleAction(Context context, String action, String source) {
        if (PlaybackAction.PREVIOUS.equals(action)) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.previous(source));
            playbackOperations.previousTrack();
        } else if (PlaybackAction.NEXT.equals(action)) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.skip(source));
            playbackOperations.nextTrack();
        } else if (PlaybackAction.TOGGLE_PLAYBACK.equals(action)) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.toggle(source, playSessionController.isPlaying()));
            playbackOperations.togglePlayback(context);
        }
    }

}
