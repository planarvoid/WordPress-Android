package com.soundcloud.android.offline;

import static java.util.Collections.unmodifiableCollection;

import com.soundcloud.android.model.Urn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class ExpectedOfflineContent {
    public static final ExpectedOfflineContent EMPTY = new ExpectedOfflineContent(
            Collections.emptyList(),
            Collections.emptyList(),
            false,
            Collections.emptyList());

    public final Collection<Urn> emptyPlaylists;
    public final Collection<DownloadRequest> requests;
    public final boolean isLikedTracksExpected;
    public final Collection<Urn> likedTracks;

    public ExpectedOfflineContent(Collection<DownloadRequest> requests,
                                  Collection<Urn> emptyPlaylists,
                                  boolean isLikedTracksExpected,
                                  List<Urn> likedTracks) {
        this.isLikedTracksExpected = isLikedTracksExpected;
        this.likedTracks = likedTracks;
        this.emptyPlaylists = unmodifiableCollection(emptyPlaylists);
        this.requests = unmodifiableCollection(requests);
    }

    public boolean isEmpty() {
        return requests.isEmpty();
    }

    @Override
    public String toString() {
        return "ExpectedOfflineContent{" +
                "emptyPlaylists=" + emptyPlaylists +
                ", requests=" + requests +
                ", isLikedTracksExpected=" + isLikedTracksExpected +
                ", likedTracks=" + likedTracks +
                '}';
    }
}
