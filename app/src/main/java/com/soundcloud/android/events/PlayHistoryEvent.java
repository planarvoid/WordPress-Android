package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import rx.functions.Func1;

@AutoValue
public abstract class PlayHistoryEvent {
    private static final int PLAY_HISTORY_ADDED = 1;

    public static final Func1<PlayHistoryEvent, Boolean> IS_PLAY_HISTORY_ADDED = new Func1<PlayHistoryEvent, Boolean>() {
        @Override
        public Boolean call(PlayHistoryEvent event) {
            return event.kind() == PLAY_HISTORY_ADDED;
        }
    };

    public static PlayHistoryEvent fromAdded(Urn trackUrn) {
        return new AutoValue_PlayHistoryEvent(PLAY_HISTORY_ADDED, trackUrn);
    }

    public abstract int kind();

    public abstract Urn urn();

}
