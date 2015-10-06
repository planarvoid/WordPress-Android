package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;

public class OfflineSyncTrackingEvent extends TrackingEvent {

    public static final String KIND_START = "start";
    public static final String KIND_FAIL = "fail";
    public static final String KIND_USER_CANCEL = "user_cancelled";
    public static final String KIND_COMPLETE = "complete";

    private final Urn track;
    private final TrackingMetadata metadata;


    private OfflineSyncTrackingEvent(String kind, Urn track, TrackingMetadata metadata) {
        super(kind, System.currentTimeMillis());
        this.track = track;
        this.metadata = metadata;
    }

    public static OfflineSyncTrackingEvent fromCompleted(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflineSyncTrackingEvent(KIND_COMPLETE, track, trackingMetadata);
    }

    public static OfflineSyncTrackingEvent fromStarted(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflineSyncTrackingEvent(KIND_START, track, trackingMetadata);
    }

    public static OfflineSyncTrackingEvent fromCancelled(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflineSyncTrackingEvent(KIND_USER_CANCEL, track, trackingMetadata);
    }

    public static OfflineSyncTrackingEvent fromFailed(Urn track, TrackingMetadata trackingMetadata) {
        return new OfflineSyncTrackingEvent(KIND_FAIL, track, trackingMetadata);
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
