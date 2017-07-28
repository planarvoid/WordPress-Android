package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;

public abstract class SearchItem {

    public enum Kind {
        USER, PLAYLIST, TRACK, HEADER
    }

    public abstract Kind kind();

    public Optional<Urn> itemUrn() {
        return Optional.absent();
    }

    @AutoValue
    public abstract static class User extends SearchItem {
        public abstract UserItem userItem();

        public abstract UiAction.UserClick clickAction();

        public Optional<Urn> itemUrn() {
            return Optional.of(userItem().getUrn());
        }

        public static User create(UserItem userItem,
                                  UiAction.UserClick clickAction) {
            return new AutoValue_SearchItem_User(Kind.USER, userItem, clickAction);
        }
    }

    @AutoValue
    public abstract static class Track extends SearchItem {

        public abstract TrackItem trackItem();

        public abstract UiAction.TrackClick clickAction();

        public Optional<Urn> itemUrn() {
            return Optional.of(trackItem().getUrn());
        }

        public static Track create(TrackItem trackItem,
                                   UiAction.TrackClick clickAction) {


            return new AutoValue_SearchItem_Track(Kind.TRACK,
                                                  trackItem,
                                                  clickAction);
        }
    }

    @AutoValue
    public abstract static class Playlist extends SearchItem {

        public abstract PlaylistItem playlistItem();

        public abstract UiAction.PlaylistClick clickAction();

        public Optional<Urn> itemUrn() {
            return Optional.of(playlistItem().getUrn());
        }

        public static Playlist create(PlaylistItem playlistItem, UiAction.PlaylistClick clickAction) {
            return new AutoValue_SearchItem_Playlist(Kind.PLAYLIST, playlistItem, clickAction);
        }
    }

    @AutoValue
    public abstract static class Header extends SearchItem {
        public abstract int bucketCategoryResource();

        public abstract int totalResults();

        public static Header create(int bucketCategoryResource, int totalResults) {
            return new AutoValue_SearchItem_Header(Kind.HEADER, bucketCategoryResource, totalResults);
        }

    }
}
