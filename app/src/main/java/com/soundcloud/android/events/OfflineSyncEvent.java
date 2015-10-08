package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineTrackContext;

public class OfflineSyncEvent extends TrackingEvent {

    public static final String KIND_SYNC = "sync";
    public static final String KIND_DESYNC = "desync";

    public static final String STAGE_START = "start";
    public static final String STAGE_FAIL = "fail";
    public static final String STAGE_COMPLETE = "complete";

    private final OfflineTrackContext trackContext;
    private final String stage;

    private OfflineSyncEvent(String kind, String stage, OfflineTrackContext trackContext) {
        super(kind, System.currentTimeMillis());
        this.stage = stage;
        this.trackContext = trackContext;
    }

    public static OfflineSyncEvent fromDesync(OfflineTrackContext trackContext) {
        return new OfflineSyncEvent(KIND_DESYNC, STAGE_COMPLETE, trackContext);
    }

    public static OfflineSyncEvent fromSyncComplete(OfflineTrackContext trackContext) {
        return new OfflineSyncEvent(KIND_SYNC, STAGE_COMPLETE, trackContext);
    }

    public static OfflineSyncEvent fromSyncStart(OfflineTrackContext trackContext) {
        return new OfflineSyncEvent(KIND_SYNC, STAGE_START, trackContext);
    }

    public static OfflineSyncEvent fromSyncFail(OfflineTrackContext trackContext) {
        return new OfflineSyncEvent(KIND_SYNC, STAGE_FAIL, trackContext);
    }

    public Urn getTrackUrn() {
        return trackContext.getTrack();
    }

    public Urn getTrackOwner() {
        return trackContext.getCreator();
    }

    public boolean isInPlaylists() {
        return !trackContext.getPlaylists().isEmpty();
    }

    public boolean isLiked() {
        return trackContext.isLiked();
    }

    public String getStage() {
        return stage;
    }
}
