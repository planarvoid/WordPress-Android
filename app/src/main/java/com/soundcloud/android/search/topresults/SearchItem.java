package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;

public abstract class SearchItem {
    public enum Kind {
        USER, PLAYLIST, TRACK, HEADER
    }

    public abstract Kind kind();

    public abstract int bucketPosition();

    public Optional<Urn> itemUrn() {
        return Optional.absent();
    }

    @AutoValue
    public static abstract class User extends SearchItem {
        public abstract UserItem userItem();

        public abstract SearchEvent.ClickSource source();

        public Optional<Urn> itemUrn() {
            return Optional.of(userItem().getUrn());
        }

        public static User create(UserItem userItem, int bucketPosition, SearchEvent.ClickSource source) {
            return new AutoValue_SearchItem_User(Kind.USER, bucketPosition, userItem, source);
        }
    }

    @AutoValue
    public static abstract class Track extends SearchItem {
        public abstract TrackItem trackItem();

        public abstract TrackSourceInfo trackSourceInfo();

        public Optional<Urn> itemUrn() {
            return Optional.of(trackItem().getUrn());
        }

        public static Track create(TrackItem trackItem, int bucketPosition, TrackSourceInfo trackSourceInfo) {
            return new AutoValue_SearchItem_Track(Kind.TRACK, bucketPosition, trackItem, trackSourceInfo);
        }
    }

    @AutoValue
    public static abstract class Playlist extends SearchItem {
        public abstract PlaylistItem playlistItem();

        public abstract SearchEvent.ClickSource source();

        public Optional<Urn> itemUrn() {
            return Optional.of(playlistItem().getUrn());
        }

        public static Playlist create(PlaylistItem playlistItem, int bucketPosition, SearchEvent.ClickSource source) {
            return new AutoValue_SearchItem_Playlist(Kind.PLAYLIST, bucketPosition, playlistItem, source);
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