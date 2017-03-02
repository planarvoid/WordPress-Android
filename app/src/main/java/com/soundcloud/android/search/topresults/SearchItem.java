package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;

public abstract class SearchItem {
    public enum Kind {
        USER, PLAYLIST, TRACK, HEADER
    }

    public abstract Kind kind();

    public abstract int bucketPosition();

    @AutoValue
    public static abstract class User extends SearchItem {
        public abstract UserItem userItem();

        public static User create(UserItem userItem, int bucketPosition) {
            return new AutoValue_SearchItem_User(Kind.USER, bucketPosition, userItem);
        }
    }

    @AutoValue
    public static abstract class Track extends SearchItem {
        public abstract TrackItem trackItem();

        public static Track create(TrackItem trackItem, int bucketPosition) {
            return new AutoValue_SearchItem_Track(Kind.TRACK, bucketPosition, trackItem);
        }
    }

    @AutoValue
    public static abstract class Playlist extends SearchItem {
        public abstract PlaylistItem playlistItem();

        public static Playlist create(PlaylistItem playlistItem, int bucketPosition) {
            return new AutoValue_SearchItem_Playlist(Kind.PLAYLIST, bucketPosition, playlistItem);
        }
    }

    @AutoValue
    public static abstract class Header extends SearchItem {
        public abstract int bucketCategoryResource();

        public abstract int totalResults();

        public static Header create(int bucketCategoryResource, int totalResults) {
            return new AutoValue_SearchItem_Header(Kind.HEADER, 0, bucketCategoryResource, totalResults);
        }

    }
}
