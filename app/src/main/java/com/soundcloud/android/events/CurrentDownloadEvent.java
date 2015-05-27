package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.DownloadState;
import rx.functions.Func1;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class CurrentDownloadEvent {
    public static final Func1<CurrentDownloadEvent, DownloadState> TO_DOWNLOAD_STATE = new Func1<CurrentDownloadEvent, DownloadState>() {
        @Override
        public DownloadState call(CurrentDownloadEvent event) {
            return event.kind;
        }
    };

    public static final Func1<CurrentDownloadEvent, Boolean> FOR_LIKED_TRACKS_FILTER = new Func1<CurrentDownloadEvent, Boolean>() {
        @Override
        public Boolean call(CurrentDownloadEvent event) {
            return event.isLikedTracks;
        }
    };

    public final DownloadState kind;
    public final Collection<Urn> entities;
    public final boolean isLikedTracks;

     private CurrentDownloadEvent(DownloadState kind, boolean isLikedTracks, List<Urn> entities) {
        this.kind = kind;
        this.entities = Collections.unmodifiableList(entities);
        this.isLikedTracks = isLikedTracks;
    }

    public static CurrentDownloadEvent idle() {
        return new CurrentDownloadEvent(DownloadState.NO_OFFLINE, false, Collections.<Urn>emptyList());
    }

    public static CurrentDownloadEvent downloaded(boolean isLikedTrack, List<Urn> urns) {
        return new CurrentDownloadEvent(DownloadState.DOWNLOADED, isLikedTrack, urns);
    }

    public static CurrentDownloadEvent downloaded(Collection<DownloadRequest> requests) {
        return create(DownloadState.DOWNLOADED, requests);
    }

    public static CurrentDownloadEvent unavailable(boolean isLikedTrack, List<Urn> urns) {
        return new CurrentDownloadEvent(DownloadState.UNAVAILABLE, isLikedTrack, urns);
    }

    public static CurrentDownloadEvent unavailable(List<DownloadRequest> requests) {
        return create(DownloadState.UNAVAILABLE, requests);
    }

    public static CurrentDownloadEvent downloading(DownloadRequest requests) {
        return create(DownloadState.DOWNLOADING, Arrays.asList(requests));
    }

    public static CurrentDownloadEvent downloadRequestRemoved(List<DownloadRequest> requests) {
        return create(DownloadState.NO_OFFLINE, requests);
    }

    public static CurrentDownloadEvent downloadRemoved(List<Urn> requests) {
        return new CurrentDownloadEvent(DownloadState.NO_OFFLINE, false, requests);
    }

    public static CurrentDownloadEvent downloadRequested(boolean isLikedTrack, List<Urn> urns) {
        return new CurrentDownloadEvent(DownloadState.REQUESTED, isLikedTrack, urns);
    }

    public static CurrentDownloadEvent downloadRequested(List<DownloadRequest> requests) {
        return create(DownloadState.REQUESTED, requests);
    }

    public static CurrentDownloadEvent offlineContentRemoved(List<Urn> urns) {
        return new CurrentDownloadEvent(DownloadState.NO_OFFLINE, true, urns);
    }

    private static CurrentDownloadEvent create(DownloadState kind, Collection<DownloadRequest> requests) {
        boolean inLikedTracks = false;
        final List<Urn> entities = new LinkedList<>();
        for (DownloadRequest request : requests) {
            inLikedTracks = inLikedTracks || request.inLikedTracks;
            entities.addAll(request.inPlaylists);
            entities.add(request.track);
        }
        return new CurrentDownloadEvent(kind, inLikedTracks, entities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrentDownloadEvent that = (CurrentDownloadEvent) o;
        return isLikedTracks == that.isLikedTracks && entities.equals(that.entities) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + entities.hashCode();
        result = 31 * result + (isLikedTracks ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CurrentDownloadEvent{" +
                "kind=" + kind +
                ", entities=" + entities +
                ", isLikedTracks=" + isLikedTracks +
                '}';
    }
}
