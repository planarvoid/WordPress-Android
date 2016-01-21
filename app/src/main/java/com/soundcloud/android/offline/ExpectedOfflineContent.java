package com.soundcloud.android.offline;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistWithTracks;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class ExpectedOfflineContent  {
    public static final ExpectedOfflineContent EMPTY = new ExpectedOfflineContent(
            Collections.<DownloadRequest>emptyList(), Collections.<PlaylistWithTracks>emptyList(),
            false, Collections.<Urn>emptyList());

    public final List<PlaylistWithTracks> offlinePlaylists;
    public final Collection<DownloadRequest> requests;
    public final boolean isLikedTracksExpected;
    public final Collection<Urn> likedTracks;

    public ExpectedOfflineContent(Collection<DownloadRequest> requests,
                                  List<PlaylistWithTracks> offlinePlaylists,
                                  boolean isLikedTracksExpected,
                                  Collection<Urn> likedTracks) {
        this.isLikedTracksExpected = isLikedTracksExpected;
        this.likedTracks = likedTracks;
        this.offlinePlaylists = unmodifiableList(offlinePlaylists);
        this.requests = unmodifiableCollection(requests);
    }
}
