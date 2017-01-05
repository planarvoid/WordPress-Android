package com.soundcloud.android.playlists;

import com.soundcloud.java.objects.MoreObjects;

class PlaylistDetailsViewModel {

    private final PlaylistWithTracks playlistWithTracks;
    private final Iterable<PlaylistDetailItem> playlistDetailItems;

    PlaylistDetailsViewModel(PlaylistWithTracks playlistWithTracks,
                             Iterable<PlaylistDetailItem> playlistDetailItems) {
        this.playlistWithTracks = playlistWithTracks;
        this.playlistDetailItems = playlistDetailItems;
    }

    public PlaylistWithTracks getPlaylistWithTracks() {
        return playlistWithTracks;
    }

    public Iterable<PlaylistDetailItem> getPlaylistDetailItems() {
        return playlistDetailItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaylistDetailsViewModel)) return false;
        PlaylistDetailsViewModel that = (PlaylistDetailsViewModel) o;
        return MoreObjects.equal(playlistWithTracks, that.playlistWithTracks) &&
                MoreObjects.equal(playlistDetailItems, that.playlistDetailItems);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(playlistWithTracks, playlistDetailItems);
    }

    @Override
    public String toString() {
        return "PlaylistDetailsViewModel{" +
                "playlistWithTracks=" + playlistWithTracks +
                ", playlistDetailItems=" + playlistDetailItems +
                '}';
    }
}
