package com.soundcloud.android.events;

import com.google.common.base.Objects;

public class PlayQueueEvent {

    public static final int NEW_QUEUE = 0;
    public static final int QUEUE_UPDATE = 1;
    public static final int AUDIO_AD_REMOVED = 2;

    private final int kind;

    public PlayQueueEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public static PlayQueueEvent fromNewQueue() {
        return new PlayQueueEvent(NEW_QUEUE);
    }

    public static PlayQueueEvent fromQueueUpdate() {
        return new PlayQueueEvent(QUEUE_UPDATE);
    }

    public static PlayQueueEvent fromAudioAdRemoved() {
        return new PlayQueueEvent(AUDIO_AD_REMOVED);
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
        final Objects.ToStringHelper stringHelper = Objects.toStringHelper(this);
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
