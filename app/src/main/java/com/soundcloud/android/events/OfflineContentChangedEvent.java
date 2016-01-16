package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class OfflineContentChangedEvent {
    public static final Func1<OfflineContentChangedEvent, OfflineState> TO_OFFLINE_STATE = new Func1<OfflineContentChangedEvent, OfflineState>() {
        @Override
        public OfflineState call(OfflineContentChangedEvent event) {
            return event.kind;
        }
    };

    public static final Func1<OfflineContentChangedEvent, Boolean> FOR_LIKED_TRACKS_FILTER = new Func1<OfflineContentChangedEvent, Boolean>() {
        @Override
        public Boolean call(OfflineContentChangedEvent event) {
            return event.isLikedTracks;
        }
    };

    public final OfflineState kind;
    public final List<Urn> entities;
    public final boolean isLikedTracks;

    private OfflineContentChangedEvent(OfflineState kind, boolean isLikedTracks, List<Urn> entities) {
        this.kind = kind;
        this.entities = Collections.unmodifiableList(entities);
        this.isLikedTracks = isLikedTracks;
    }

    public static OfflineContentChangedEvent idle() {
        return new OfflineContentChangedEvent(OfflineState.NOT_OFFLINE, false, Collections.<Urn>emptyList());
    }

    public static OfflineContentChangedEvent downloaded(boolean isLikedTrack, List<Urn> urns) {
        return new OfflineContentChangedEvent(OfflineState.DOWNLOADED, isLikedTrack, urns);
    }

    public static OfflineContentChangedEvent downloaded(List<DownloadRequest> requests) {
        return create(OfflineState.DOWNLOADED, requests);
    }

    public static OfflineContentChangedEvent unavailable(List<DownloadRequest> requests) {
        return create(OfflineState.UNAVAILABLE, requests);
    }

    public static OfflineContentChangedEvent downloading(DownloadRequest requests) {
        return create(OfflineState.DOWNLOADING, Arrays.asList(requests));
    }

    public static OfflineContentChangedEvent downloadRequestRemoved(List<DownloadRequest> requests) {
        return create(OfflineState.NOT_OFFLINE, requests);
    }

    public static OfflineContentChangedEvent downloadRemoved(List<Urn> requests) {
        return new OfflineContentChangedEvent(OfflineState.NOT_OFFLINE, false, requests);
    }

    public static OfflineContentChangedEvent downloadRequested(boolean isLikedTrack, List<Urn> urns) {
        return new OfflineContentChangedEvent(OfflineState.REQUESTED, isLikedTrack, urns);
    }

    public static OfflineContentChangedEvent downloadRequested(List<DownloadRequest> requests) {
        return create(OfflineState.REQUESTED, requests);
    }

    public static OfflineContentChangedEvent offlineContentRemoved(List<Urn> urns) {
        return new OfflineContentChangedEvent(OfflineState.NOT_OFFLINE, true, urns);
    }

    private static OfflineContentChangedEvent create(OfflineState kind, List<DownloadRequest> requests) {
        boolean isLiked = false;
        final List<Urn> entities = new ArrayList<>();
        for (DownloadRequest request : requests) {
            isLiked = isLiked || request.isLiked();
            entities.addAll(request.getPlaylists());
            entities.add(request.getTrack());
        }
        return new OfflineContentChangedEvent(kind, isLiked, entities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OfflineContentChangedEvent that = (OfflineContentChangedEvent) o;
        return MoreObjects.equal(kind, that.kind)
                && MoreObjects.equal(isLikedTracks, that.isLikedTracks)
                && MoreObjects.equal(entities, that.entities);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(kind, entities, isLikedTracks);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("kind", kind)
                .add("entities", entities)
                .add("isLikedTracks", isLikedTracks).toString();
    }
}
