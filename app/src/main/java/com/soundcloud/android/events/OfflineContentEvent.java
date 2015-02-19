package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

public class OfflineContentEvent {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STOP = 2;
    public static final int QUEUE_UPDATED = 3; //?

    private final int kind;
    private final Urn downloadedItem;

    public OfflineContentEvent(int kind, Urn downloadedItem) {
        this.kind = kind;
        this.downloadedItem = downloadedItem;
    }

    public static OfflineContentEvent idle() {
        return new OfflineContentEvent(IDLE, Urn.NOT_SET);
    }

    public static OfflineContentEvent start() {
        return new OfflineContentEvent(START, Urn.NOT_SET);
    }

    public static OfflineContentEvent stop() {
        return new OfflineContentEvent(STOP, Urn.NOT_SET);
    }

    // is that really needed?
    public static OfflineContentEvent queueUpdate() {
        return new OfflineContentEvent(QUEUE_UPDATED, Urn.NOT_SET);
    }

    public int getKind() {
        return kind;
    }

    public Urn getUrn() {
        return downloadedItem;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("kind", getKindName()).toString();
    }

    private String getKindName() {
        switch (getKind()){
            case IDLE: return "IDLE";
            case START: return "START";
            case STOP: return "STOP";
            case QUEUE_UPDATED: return "QUEUE_UPDATED";
            default: return "unknown";
        }
    }
}
