package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;

@AutoValue
public abstract class CurrentPlayQueueItemEvent {
    private static final int NEW_QUEUE = 0;
    private static final int POSITION_CHANGED = 1;

    public static CurrentPlayQueueItemEvent fromNewQueue(PlayQueueItem playQueueItem, Urn collectionUrn, int position) {
        return new AutoValue_CurrentPlayQueueItemEvent(NEW_QUEUE, playQueueItem, collectionUrn, position);
    }

    public static CurrentPlayQueueItemEvent fromPositionChanged(PlayQueueItem playQueueItem, Urn collectionUrn, int position) {
        return new AutoValue_CurrentPlayQueueItemEvent(POSITION_CHANGED, playQueueItem, collectionUrn, position);
    }

    public abstract int getKind();

    public abstract PlayQueueItem getCurrentPlayQueueItem();

    public abstract Urn getCollectionUrn();

    public abstract int getPosition();
}
