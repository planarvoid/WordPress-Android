package com.soundcloud.android.playback.external;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class PlaybackActionController {

    private final PlaybackOperations playbackOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EventBus eventBus;

    @Inject
    public PlaybackActionController(PlaybackOperations playbackOperations, PlaySessionStateProvider playSessionStateProvider, EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    public void handleAction(String action, String source) {
        if (PlaybackAction.PLAY.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.play(source));
            playbackOperations.play();
        } else if (PlaybackAction.PAUSE.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.pause(source));
            playbackOperations.pause();
        } else if (PlaybackAction.PREVIOUS.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.previous(source));
            playbackOperations.previousTrack();
        } else if (PlaybackAction.NEXT.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skip(source));
            playbackOperations.nextTrack();
        } else if (PlaybackAction.TOGGLE_PLAYBACK.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.toggle(source, playSessionStateProvider.isPlaying()));
            playbackOperations.togglePlayback();
        } else if (PlaybackAction.CLOSE.equals(action)) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.close(source));
            playbackOperations.stopService();
        }
    }

}
