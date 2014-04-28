package com.soundcloud.android.events;

import com.soundcloud.android.model.Playable;

public final class PlayableChangedEvent {

    private final Playable playable;

    public static PlayableChangedEvent create(Playable playable) {
        return new PlayableChangedEvent(playable);
    }

    private PlayableChangedEvent(Playable playable) {
        this.playable = playable;
    }

    public Playable getPlayable() {
        return playable;
    }
}
