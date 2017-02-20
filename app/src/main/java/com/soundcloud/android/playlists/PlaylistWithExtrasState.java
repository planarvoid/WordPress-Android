package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.VisibleForTesting;

import java.util.List;

@VisibleForTesting
@AutoValue
abstract class PlaylistWithExtrasState {

    static PlaylistWithExtrasState initialState() {
        return builder().build();
    }

    static Builder builder() {
        return new AutoValue_PlaylistWithExtrasState.Builder()
                .isRefreshing(false)
                .refreshError(Optional.absent())
                .playlistWithExtras(Optional.absent())
                .viewError(Optional.absent());
    }

    abstract Optional<PlaylistWithExtras> playlistWithExtras();

    abstract Optional<ViewError> viewError();

    abstract Optional<ViewError> refreshError();

    abstract boolean isRefreshing();

    abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder playlistWithExtras(Optional<PlaylistWithExtras> value);

        abstract Builder viewError(Optional<ViewError> value);

        abstract Builder refreshError(Optional<ViewError> value);

        abstract Builder isRefreshing(boolean value);

        abstract PlaylistWithExtrasState build();

    }

    interface PartialState {

        PlaylistWithExtrasState newState(PlaylistWithExtrasState oldState);

        class RefreshStarted implements PartialState {
            @Override
            public PlaylistWithExtrasState newState(PlaylistWithExtrasState oldState) {
                return oldState.toBuilder().isRefreshing(true).refreshError(Optional.absent()).build();
            }
        }

        class RefreshError implements PartialState {

            private final Throwable viewError;

            RefreshError(Throwable viewError) {
                this.viewError = viewError;
            }

            @Override
            public PlaylistWithExtrasState newState(PlaylistWithExtrasState oldState) {
                return oldState.toBuilder()
                               .refreshError(Optional.of(ViewError.from(viewError)))
                               .isRefreshing(false)
                               .build();
            }
        }

        class LoadingError implements PartialState {

            private final Throwable viewError;

            LoadingError(Throwable viewError) {
                this.viewError = viewError;
            }

            @Override
            public PlaylistWithExtrasState newState(PlaylistWithExtrasState oldState) {
                return oldState.toBuilder()
                               .viewError(Optional.of(ViewError.from(viewError)))
                               .isRefreshing(false)
                               .refreshError(Optional.absent())
                               .build();
            }
        }

        class PlaylistWithExtrasLoaded implements PartialState {

            private final PlaylistWithExtras playlistWithExtras;

            PlaylistWithExtrasLoaded(Playlist playlist, Optional<List<Track>> trackList, List<Playlist> otherPlaylistsOpt) {
                this.playlistWithExtras = PlaylistWithExtras.create(playlist, trackList, otherPlaylistsOpt);
            }

            @Override
            public PlaylistWithExtrasState newState(PlaylistWithExtrasState oldState) {
                return oldState.toBuilder()
                               .viewError(Optional.absent())
                               .refreshError(Optional.absent())
                               .isRefreshing(false)
                               .playlistWithExtras(Optional.of(playlistWithExtras))
                               .build();
            }
        }


    }
}
