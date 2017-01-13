package com.soundcloud.android.events;

import static com.soundcloud.android.events.OfflinePerformanceEvent.Kind.KIND_COMPLETE;
import static com.soundcloud.android.events.OfflinePerformanceEvent.Kind.KIND_FAIL;
import static com.soundcloud.android.events.OfflinePerformanceEvent.Kind.KIND_START;
import static com.soundcloud.android.events.OfflinePerformanceEvent.Kind.KIND_STORAGE_LIMIT;
import static com.soundcloud.android.events.OfflinePerformanceEvent.Kind.KIND_USER_CANCEL;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class OfflinePerformanceEvent extends NewTrackingEvent {

    public enum Kind {
        KIND_START("start"),
        KIND_FAIL("fail"),
        KIND_USER_CANCEL("user_cancelled"),
        KIND_COMPLETE("complete"),
        KIND_STORAGE_LIMIT("storage_limit_reached");
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    private static OfflinePerformanceEvent create(Kind kind, Urn track, TrackingMetadata metadata) {
        return new AutoValue_OfflinePerformanceEvent(defaultId(), defaultTimestamp(), Optional.absent(), kind, track, metadata.getCreatorUrn(), metadata.isFromPlaylists(), metadata.isFromLikes());
    }

    public static OfflinePerformanceEvent fromCompleted(Urn track, TrackingMetadata trackingMetadata) {
        return create(KIND_COMPLETE, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromStarted(Urn track, TrackingMetadata trackingMetadata) {
        return create(KIND_START, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromCancelled(Urn track, TrackingMetadata trackingMetadata) {
        return create(KIND_USER_CANCEL, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromFailed(Urn track, TrackingMetadata trackingMetadata) {
        return create(KIND_FAIL, track, trackingMetadata);
    }

    public static OfflinePerformanceEvent fromStorageLimit(Urn track, TrackingMetadata trackingMetadata) {
        return create(KIND_STORAGE_LIMIT, track, trackingMetadata);
    }

    public abstract Kind kind();

    public abstract Urn trackUrn();

    public abstract Urn trackOwner();

    public abstract boolean partOfPlaylist();

    public abstract boolean isFromLikes();

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_OfflinePerformanceEvent(id(), timestamp(), Optional.of(referringEvent), kind(), trackUrn(), trackOwner(), partOfPlaylist(), isFromLikes());
    }
}
