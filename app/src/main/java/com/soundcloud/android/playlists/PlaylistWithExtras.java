package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.List;

@VisibleForTesting
@AutoValue
abstract class PlaylistWithExtras {

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks) {
        return create(playlist,tracks, Collections.emptyList());
    }

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks, List<Playlist> otherPlaylistsByCreator) {
        return  new AutoValue_PlaylistWithExtras.Builder()
                .playlist(playlist)
                .tracks(tracks)
                .otherPlaylistsByCreator(otherPlaylistsByCreator)
                .build();
    }

    abstract Playlist playlist();

    abstract Optional<List<Track>> tracks();

    abstract List<Playlist> otherPlaylistsByCreator();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder playlist(Playlist value);

        abstract Builder tracks(Optional<List<Track>> value);

        abstract Builder otherPlaylistsByCreator(List<Playlist> value);

        abstract PlaylistWithExtras build();

    }

    @Override
    public String toString() {
        return "PlaylistWithExtras{ "+ playlist().title() + " " + tracks() + " " + otherPlaylistsByCreator() + " }";
    }
}
