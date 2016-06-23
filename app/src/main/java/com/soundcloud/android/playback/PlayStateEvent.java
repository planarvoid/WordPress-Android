package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.strings.Strings;

@AutoValue
public abstract class PlayStateEvent {

    public static PlayStateEvent DEFAULT = create(PlaybackStateTransition.DEFAULT, 0, false, Strings.EMPTY);

    public static PlayStateEvent create(PlaybackStateTransition transition,
                                        long apiDuration,
                                        boolean isFirstPlay,
                                        String playId) {
        return new AutoValue_PlayStateEvent(transition.getUrn(), transition, getValidProgress(transition, apiDuration), isFirstPlay, playId, false);
    }

    public static PlayStateEvent createPlayQueueCompleteEvent(PlayStateEvent fromEvent) {
        return new AutoValue_PlayStateEvent(fromEvent.getPlayingItemUrn(),
                                            fromEvent.getTransition(),
                                            fromEvent.getProgress(),
                                            false,
                                            fromEvent.getPlayId(),
                                            true);
    }

    public boolean playSessionIsActive() {
        return getTransition().playSessionIsActive();
    }

    public boolean isPlayerIdle() {
        return getTransition().isPlayerIdle();
    }

    public boolean playbackEnded() {
        return getTransition().playbackEnded();
    }

    public boolean isPlayerPlaying() {
        return getTransition().isPlayerPlaying();
    }

    public boolean isPaused() {
        return getTransition().isPaused();
    }

    public boolean isBuffering() {
        return getTransition().isBuffering();
    }

    public PlaybackState getNewState() {
        return getTransition().getNewState();
    }

    public PlayStateReason getReason() {
        return getTransition().getReason();
    }

    private static PlaybackProgress getValidProgress(PlaybackStateTransition stateTransition, long apiDuration) {
        final PlaybackProgress progress = stateTransition.getProgress();
        return progress.isDurationValid() ? progress : PlaybackProgress.withDuration(progress, apiDuration);
    }

    public abstract Urn getPlayingItemUrn();

    public abstract PlaybackStateTransition getTransition();

    public abstract PlaybackProgress getProgress();

    public abstract boolean isFirstPlay();

    public abstract String getPlayId();

    public abstract boolean isPlayQueueComplete();

}
