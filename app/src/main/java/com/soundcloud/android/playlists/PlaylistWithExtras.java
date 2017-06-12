package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

@AutoValue
abstract class PlaylistWithExtras {

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks, boolean isOwner) {
        return create(playlist, tracks, Collections.emptyList(), isOwner);
    }

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks, List<Playlist> otherPlaylistsByCreator, boolean isOwner) {
        return  new AutoValue_PlaylistWithExtras.Builder()
                .playlist(playlist)
                .tracks(tracks)
                .otherPlaylistsByCreator(otherPlaylistsByCreator)
                .isLoggedInUserOwner(isOwner)
                .build();
    }

    abstract Playlist playlist();

    abstract Optional<List<Track>> tracks();

    abstract List<Playlist> otherPlaylistsByCreator();

    abstract boolean isLoggedInUserOwner();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder playlist(Playlist value);

        abstract Builder tracks(Optional<List<Track>> value);

        abstract Builder otherPlaylistsByCreator(List<Playlist> value);

        abstract Builder isLoggedInUserOwner(boolean value);

        abstract PlaylistWithExtras build();

    }
}
