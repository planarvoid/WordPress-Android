package com.soundcloud.android.playlists;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.VisibleForTesting;

import java.util.List;

@VisibleForTesting
@AutoValue
abstract class PlaylistWithExtras {

    static PlaylistWithExtras create(Playlist playlist, List<Track> tracks) {
        return create(playlist, Optional.of(tracks), absent());
    }

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks) {
        return create(playlist, tracks, absent());
    }

    static PlaylistWithExtras create(Playlist playlist, List<Track> tracks, List<Playlist> otherPlaylistsByCreator) {
        return create(playlist, Optional.of(tracks), of(otherPlaylistsByCreator));
    }

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks, List<Playlist> otherPlaylistsByCreator) {
        return create(playlist, tracks, of(otherPlaylistsByCreator));
    }

    static PlaylistWithExtras create(Playlist playlist, Optional<List<Track>> tracks, Optional<List<Playlist>> otherPlaylistsByCreator) {
        return  new AutoValue_PlaylistWithExtras.Builder()
                .playlist(playlist)
                .tracks(tracks)
                .otherPlaylistsByCreator(otherPlaylistsByCreator)
                .build();
    }

    abstract Playlist playlist();

    abstract Optional<List<Track>> tracks();

    abstract Optional<List<Playlist>> otherPlaylistsByCreator();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder playlist(Playlist value);

        abstract Builder tracks(Optional<List<Track>> value);

        abstract Builder otherPlaylistsByCreator(Optional<List<Playlist>> value);

        abstract PlaylistWithExtras build();

    }

    @Override
    public String toString() {
        return "PlaylistWithExtras{ "+ playlist().title() + " " + tracks() + " " + otherPlaylistsByCreator().isPresent() + " }";
    }
}
