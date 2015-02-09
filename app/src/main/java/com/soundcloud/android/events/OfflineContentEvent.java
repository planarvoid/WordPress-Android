package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

public class OfflineContentEvent {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STOP = 2;


    public static final int DOWNLOAD_STARTED = 3;
    public static final int DOWNLOAD_FINISHED = 4;
    public static final int DOWNLOAD_FAILED = 5;
    public static final int QUEUE_UPDATED = 6; //?

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

    public static OfflineContentEvent downloadStarted(Urn track) {
        return new OfflineContentEvent(DOWNLOAD_STARTED, track);
    }

    public static OfflineContentEvent downloadFinished(Urn track) {
        return new OfflineContentEvent(DOWNLOAD_FINISHED, track);
    }

    public static OfflineContentEvent downloadFailed(Urn track) {
        return new OfflineContentEvent(DOWNLOAD_FAILED, track);
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
            case DOWNLOAD_STARTED: return "DOWNLOAD_STARTED";
            case DOWNLOAD_FINISHED: return "DOWNLOAD_FINISHED";
            case DOWNLOAD_FAILED: return "DOWNLOAD_FAILED";
            case QUEUE_UPDATED: return "QUEUE_UPDATED";
            default: return "unknown";
        }
    }
}
