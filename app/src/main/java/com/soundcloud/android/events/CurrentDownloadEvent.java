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

public final class CurrentDownloadEvent {
    public static final Func1<CurrentDownloadEvent, OfflineState> TO_OFFLINE_STATE = new Func1<CurrentDownloadEvent, OfflineState>() {
        @Override
        public OfflineState call(CurrentDownloadEvent event) {
            return event.kind;
        }
    };

    public static final Func1<CurrentDownloadEvent, Boolean> FOR_LIKED_TRACKS_FILTER = new Func1<CurrentDownloadEvent, Boolean>() {
        @Override
        public Boolean call(CurrentDownloadEvent event) {
            return event.isLikedTracks;
        }
    };

    public final OfflineState kind;
    public final List<Urn> entities;
    public final boolean isLikedTracks;

    private CurrentDownloadEvent(OfflineState kind, boolean isLikedTracks, List<Urn> entities) {
        this.kind = kind;
        this.entities = Collections.unmodifiableList(entities);
        this.isLikedTracks = isLikedTracks;
    }

    public static CurrentDownloadEvent idle() {
        return new CurrentDownloadEvent(OfflineState.NO_OFFLINE, false, Collections.<Urn>emptyList());
    }

    public static CurrentDownloadEvent downloaded(boolean isLikedTrack, List<Urn> urns) {
        return new CurrentDownloadEvent(OfflineState.DOWNLOADED, isLikedTrack, urns);
    }

    public static CurrentDownloadEvent downloaded(List<DownloadRequest> requests) {
        return create(OfflineState.DOWNLOADED, requests);
    }

    public static CurrentDownloadEvent unavailable(boolean isLikedTrack, List<Urn> urns) {
        return new CurrentDownloadEvent(OfflineState.UNAVAILABLE, isLikedTrack, urns);
    }

    public static CurrentDownloadEvent unavailable(List<DownloadRequest> requests) {
        return create(OfflineState.UNAVAILABLE, requests);
    }

    public static CurrentDownloadEvent downloading(DownloadRequest requests) {
        return create(OfflineState.DOWNLOADING, Arrays.asList(requests));
    }

    public static CurrentDownloadEvent downloadRequestRemoved(List<DownloadRequest> requests) {
        return create(OfflineState.NO_OFFLINE, requests);
    }

    public static CurrentDownloadEvent downloadRemoved(List<Urn> requests) {
        return new CurrentDownloadEvent(OfflineState.NO_OFFLINE, false, requests);
    }

    public static CurrentDownloadEvent downloadRequested(boolean isLikedTrack, List<Urn> urns) {
        return new CurrentDownloadEvent(OfflineState.REQUESTED, isLikedTrack, urns);
    }

    public static CurrentDownloadEvent downloadRequested(List<DownloadRequest> requests) {
        return create(OfflineState.REQUESTED, requests);
    }

    public static CurrentDownloadEvent offlineContentRemoved(List<Urn> urns) {
        return new CurrentDownloadEvent(OfflineState.NO_OFFLINE, true, urns);
    }

    private static CurrentDownloadEvent create(OfflineState kind, List<DownloadRequest> requests) {
        boolean isLiked = false;
        final List<Urn> entities = new ArrayList<>();
        for (DownloadRequest request : requests) {
            isLiked = isLiked || request.isLiked();
            entities.addAll(request.getPlaylists());
            entities.add(request.getTrack());
        }
        return new CurrentDownloadEvent(kind, isLiked, entities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrentDownloadEvent that = (CurrentDownloadEvent) o;
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
