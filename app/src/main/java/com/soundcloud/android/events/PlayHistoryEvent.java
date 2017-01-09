package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import rx.functions.Func1;

@AutoValue
public abstract class PlayHistoryEvent {
    private static final int PLAY_HISTORY_ADDED = 1;
    private static final int PLAY_HISTORY_UPDATED = 2;

    public static final Func1<PlayHistoryEvent, Boolean> IS_PLAY_HISTORY_CHANGE = event -> event.kind() == PLAY_HISTORY_ADDED || event.kind() == PLAY_HISTORY_UPDATED;

    public static PlayHistoryEvent fromAdded(Urn trackUrn) {
        return new AutoValue_PlayHistoryEvent(PLAY_HISTORY_ADDED, trackUrn);
    }

    public static PlayHistoryEvent updated() {
        return new AutoValue_PlayHistoryEvent(PLAY_HISTORY_UPDATED, Urn.NOT_SET);
    }

    public abstract int kind();

    public abstract Urn urn();

}
