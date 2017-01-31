package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;

public abstract class SearchItem {
    public enum Kind {
        USER, PLAYLIST, TRACK
    }
    public abstract Kind kind();

    public abstract Urn urn();

    public abstract SearchItem withLikeStatus(LikesStatusEvent.LikeStatus likeStatus);

    @AutoValue
    public static abstract class User extends SearchItem {
        public abstract UserItem userItem();
        public static User create(UserItem userItem) {
            return new AutoValue_SearchItem_User(Kind.USER, userItem.getUrn(), userItem);
        }

        @Override
        public SearchItem withLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
            return this;
        }
    }
    @AutoValue
    public static abstract class Track extends SearchItem {
        public abstract TrackItem trackItem();
        public static Track create(TrackItem trackItem) {
            return new AutoValue_SearchItem_Track(Kind.TRACK, trackItem.getUrn(), trackItem);
        }

        @Override
        public SearchItem withLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
            return create(trackItem().updatedWithLike(likeStatus));
        }
    }
    @AutoValue
    public static abstract class Playlist extends SearchItem {
        public abstract PlaylistItem playlistItem();
        public static Playlist create(PlaylistItem playlistItem) {
            return new AutoValue_SearchItem_Playlist(Kind.PLAYLIST, playlistItem.getUrn(), playlistItem);
        }


        @Override
        public SearchItem withLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
            return create(playlistItem().updatedWithLike(likeStatus));
        }
    }
}
