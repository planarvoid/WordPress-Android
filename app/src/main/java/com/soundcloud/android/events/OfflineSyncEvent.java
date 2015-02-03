package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadResult;

public class OfflineSyncEvent {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STOP = 2;
    public static final int DOWNLOAD_FINISHED = 3;
    public static final int QUEUE_UPDATED = 4;

    private final int kind;
    private final Urn downloadedItem;

    public OfflineSyncEvent(int kind, Urn downloadedItem) {
        this.kind = kind;
        this.downloadedItem = downloadedItem;
    }

    public static OfflineSyncEvent idle() {
        return new OfflineSyncEvent(IDLE, Urn.NOT_SET);
    }

    public static OfflineSyncEvent start() {
        return new OfflineSyncEvent(START, Urn.NOT_SET);
    }

    public static OfflineSyncEvent stop() {
        return new OfflineSyncEvent(STOP, Urn.NOT_SET);
    }

    public static OfflineSyncEvent progress(DownloadResult downloadResult) {
        return new OfflineSyncEvent(DOWNLOAD_FINISHED, downloadResult.getUrn());
    }

    public static OfflineSyncEvent queueUpdate() {
        return new OfflineSyncEvent(QUEUE_UPDATED, Urn.NOT_SET);
    }

    public int getKind() {
        return kind;
    }

    public Urn getDownloadedItem() {
        return downloadedItem;
    }
}
