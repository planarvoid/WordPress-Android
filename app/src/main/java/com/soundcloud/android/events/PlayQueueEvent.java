package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

public class PlayQueueEvent {

    public static final int NEW_QUEUE = 0;
    public static final int QUEUE_UPDATE = 1;
    public static final int AUDIO_AD_REMOVED = 2;

    private final int kind;
    private final Urn collectionUrn;

    public PlayQueueEvent(int kind, Urn collectionUrn) {
        this.kind = kind;
        this.collectionUrn = collectionUrn;
    }

    public int getKind() {
        return kind;
    }

    public Urn getCollectionUrn() { return  collectionUrn; }

    public static PlayQueueEvent fromNewQueue(Urn collectionUrn) {
        return new PlayQueueEvent(NEW_QUEUE, collectionUrn);
    }

    public static PlayQueueEvent fromQueueUpdate(Urn collectionUrn) {
        return new PlayQueueEvent(QUEUE_UPDATE, collectionUrn);
    }

    public static PlayQueueEvent fromAudioAdRemoved(Urn collectionUrn) {
        return new PlayQueueEvent(AUDIO_AD_REMOVED, collectionUrn);
    }

    public boolean isQueueUpdate() {
        return kind == QUEUE_UPDATE;
    }

    public boolean isNewQueue() {
        return kind == NEW_QUEUE;
    }

    public boolean audioAdRemoved() {
        return kind == AUDIO_AD_REMOVED;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayQueueEvent
                && ((PlayQueueEvent) o).getKind() == kind;
    }

    @Override
    public int hashCode() {
        return kind;
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper stringHelper = MoreObjects.toStringHelper(this);
        return stringHelper.add("kind", getKindName()).toString();
    }

    private String getKindName() {
        switch (kind) {
            case NEW_QUEUE:
                return "NEW_QUEUE";
            case QUEUE_UPDATE:
                return "QUEUE_UPDATE";
            case AUDIO_AD_REMOVED:
                return "AUDIO_AD_REMOVED";
            default:
                return "unknown";
        }
    }
}
