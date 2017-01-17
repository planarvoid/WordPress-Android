package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class PlaylistDetailsViewModel {

    @Deprecated
    static PlaylistDetailsViewModel create(PlaylistWithTracks playlistWithTracks,
                             Iterable<PlaylistDetailItem> playlistDetailItems) {
        return create(playlistWithTracks, playlistDetailItems, playlistWithTracks.isLikedByUser());
    }

    static PlaylistDetailsViewModel create(PlaylistWithTracks playlistWithTracks,
                             Iterable<PlaylistDetailItem> playlistDetailItems,
                             boolean isLiked) {
        return new AutoValue_PlaylistDetailsViewModel(playlistWithTracks, playlistDetailItems, isLiked);
    }

    abstract PlaylistWithTracks playlistWithTracks();

    abstract Iterable<PlaylistDetailItem> playlistDetailItems();

    abstract boolean isLiked();
}
