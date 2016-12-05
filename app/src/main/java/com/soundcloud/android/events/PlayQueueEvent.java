package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class PlayQueueEvent {

    private static final int NEW_QUEUE = 0;
    private static final int QUEUE_UPDATE = 1;
    private static final int ADS_REMOVED = 2;
    private static final int AUTO_PLAY_ENABLED = 3;
    private static final int QUEUE_REORDER = 4;

    public abstract boolean itemMoved();

    public abstract boolean itemRemoved();

    public abstract boolean itemAdded();

    public abstract int getKind();

    public abstract Urn getCollectionUrn();

    public static PlayQueueEvent fromNewQueue(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, false, NEW_QUEUE, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdate(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, false, QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromAdsRemoved(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, false, ADS_REMOVED, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdateMoved(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(true, false, false, QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdateRemoved(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, true, false, QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromQueueInsert(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, true, QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromAutoPlayEnabled(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, true, AUTO_PLAY_ENABLED, collectionUrn);
    }

    public static PlayQueueEvent fromQueueReordered(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, false, QUEUE_REORDER, collectionUrn);
    }

    public boolean isQueueUpdate() {
        return getKind() == QUEUE_UPDATE;
    }

    public boolean isNewQueue() {
        return getKind() == NEW_QUEUE;
    }

    public boolean adsRemoved() {
        return getKind() == ADS_REMOVED;
    }

    public boolean itemChanged() {
        return itemRemoved() || itemMoved();
    }

    public boolean isAutoPlayEnabled() {
        return getKind() == AUTO_PLAY_ENABLED;
    }

    public boolean isQueueReorder() {
        return getKind() == QUEUE_REORDER;
    }

}
