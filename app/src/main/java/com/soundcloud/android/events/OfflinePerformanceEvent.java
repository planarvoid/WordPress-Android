package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;

public class OfflinePerformanceEvent extends LegacyTrackingEvent {

    public static final String KIND_START = "start";
    public static final String KIND_FAIL = "fail";
    public static final String KIND_USER_CANCEL = "user_cancelled";
    public static final String KIND_COMPLETE = "complete";
    public static final String KIND_STORAGE_LIMIT = "storage_limit_reached";

    private final Urn track;
    private final TrackingMetadata metadata;


    private OfflinePerformanceEvent(String kind, Urn track, TrackingMetadata metadata) {
        super(kind);
        this.track = track;
        this.metadata = metadata;
    }

    public static OfflinePerformanceEvent fromCompleted(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflinePerformanceEvent(KIND_COMPLETE, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromStarted(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflinePerformanceEvent(KIND_START, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromCancelled(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflinePerformanceEvent(KIND_USER_CANCEL, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromFailed(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflinePerformanceEvent(KIND_FAIL, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromStorageLimit(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflinePerformanceEvent(KIND_STORAGE_LIMIT, track, trackingMetadata);
    }

    public Urn getTrackUrn() {
        return track;
    }

    public Urn getTrackOwner() {
        return metadata.getCreatorUrn();
    }

    public boolean partOfPlaylist() {
        return metadata.isFromPlaylists();
    }

    public boolean isFromLikes() {
        return metadata.isFromLikes();
    }
}
