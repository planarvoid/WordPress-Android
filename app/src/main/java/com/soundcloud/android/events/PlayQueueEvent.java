package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class PlayQueueEvent {

    public static final int NEW_QUEUE = 0;
    public static final int QUEUE_UPDATE = 1;
    public static final int ADS_REMOVED = 2;

    public abstract boolean itemMoved();

    public abstract boolean itemRemoved();

    public abstract int getKind();

    public abstract Urn getCollectionUrn();

    public static PlayQueueEvent fromNewQueue(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, NEW_QUEUE, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdate(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromAdsRemoved(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, false, ADS_REMOVED, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdateMoved(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(true, false, QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdateRemoved(Urn collectionUrn) {
        return new AutoValue_PlayQueueEvent(false, true, QUEUE_UPDATE, collectionUrn);
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


}