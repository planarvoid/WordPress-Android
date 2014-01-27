package com.soundcloud.android.events;

import com.soundcloud.android.model.Playable;

public final class PlayableChangedEvent implements Event {

    private Playable mPlayable;

    public static PlayableChangedEvent create(Playable playable) {
        return new PlayableChangedEvent(playable);
    }

    private PlayableChangedEvent(Playable playable) {
        mPlayable = playable;
    }

    public Playable getPlayable() {
        return mPlayable;
    }

    @Override
    public int getKind() {
        return 0;
    }
}
