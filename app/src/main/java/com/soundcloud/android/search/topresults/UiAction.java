package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;

import java.util.UUID;

abstract class UiAction {
    @AutoValue
    abstract static class TrackClick extends UiAction {
        abstract String searchQuery();

        abstract SearchItem.Track clickedTrack();

        static TrackClick create(String searchQuery, SearchItem.Track track) {
            return new AutoValue_UiAction_TrackClick(searchQuery, track);
        }
    }

    @AutoValue
    abstract static class PlaylistClick extends UiAction {
        abstract String searchQuery();

        abstract SearchItem.Playlist clickedPlaylist();

        static PlaylistClick create(String searchQuery, SearchItem.Playlist playlist) {
            return new AutoValue_UiAction_PlaylistClick(searchQuery, playlist);
        }
    }

    @AutoValue
    abstract static class UserClick extends UiAction {
        abstract String searchQuery();

        abstract SearchItem.User clickedUser();

        static UserClick create(String searchQuery, SearchItem.User user) {
            return new AutoValue_UiAction_UserClick(searchQuery, user);
        }
    }

    @AutoValue
    abstract static class ViewAllClick extends UiAction {
        abstract String searchQuery();

        abstract TopResults.Bucket.Kind bucketKind();

        static ViewAllClick create(String searchQuery, TopResults.Bucket.Kind bucketKind) {
            return new AutoValue_UiAction_ViewAllClick(searchQuery, bucketKind);
        }
    }

    @AutoValue
    abstract static class HelpClick extends UiAction {
        static HelpClick create() {
            return new AutoValue_UiAction_HelpClick();
        }
    }

    @AutoValue
    abstract static class Search extends UiAction {
        abstract SearchParams searchParams();

        static Search create(SearchParams searchParams) {
            return new AutoValue_UiAction_Search(searchParams);
        }
    }

    @AutoValue
    abstract static class Refresh extends UiAction {
        abstract SearchParams searchParams();

        static Refresh create(SearchParams searchParams) {
            return new AutoValue_UiAction_Refresh(searchParams);
        }
    }

    @AutoValue
    abstract static class Enter extends UiAction {
        abstract String uuid();
        abstract String searchQuery();

        static Enter create(String searchQuery) {
            return new AutoValue_UiAction_Enter(UUID.randomUUID().toString(), searchQuery);
        }
    }
}
