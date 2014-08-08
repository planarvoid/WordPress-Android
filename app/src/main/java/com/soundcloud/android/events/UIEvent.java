package com.soundcloud.android.events;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class UIEvent {

    public enum Kind {
        FOLLOW,
        UNFOLLOW,
        LIKE,
        UNLIKE,
        REPOST,
        UNREPOST,
        ADD_TO_PLAYLIST,
        COMMENT,
        SHARE,
        SHUFFLE_LIKES,
        NAVIGATION
    }

    private final Kind kind;
    private final Map<String, String> attributes;

    public static UIEvent fromToggleFollow(boolean isFollow, String screenTag, long userId) {
        return new UIEvent(isFollow ? Kind.FOLLOW : Kind.UNFOLLOW)
                .putAttribute("context", screenTag)
                .putAttribute("user_id", String.valueOf(userId));
    }

    public static UIEvent fromToggleLike(boolean isLike, String screenTag, @NotNull Playable playable) {
        return new UIEvent(isLike ? Kind.LIKE : Kind.UNLIKE)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromToggleRepost(boolean isRepost, String screenTag, @NotNull Playable playable) {
        return new UIEvent(isRepost ? Kind.REPOST : Kind.UNREPOST)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        return new UIEvent(Kind.ADD_TO_PLAYLIST)
                .putAttribute("context", screenTag)
                .putAttribute("is_new_playlist", isNewPlaylist ? "yes" : "no")
                .putAttribute("track_id", String.valueOf(trackId));
    }

    public static UIEvent fromComment(String screenTag, long trackId) {
        return new UIEvent(Kind.COMMENT)
                .putAttribute("context", screenTag)
                .putAttribute("track_id", String.valueOf(trackId));
    }

    public static UIEvent fromShare(String screenTag, @NotNull Playable playable) {
        return new UIEvent(Kind.SHARE)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(Kind.SHUFFLE_LIKES);
    }

    public static UIEvent fromProfileNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "you");
    }

    public static UIEvent fromStreamNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "stream");
    }

    public static UIEvent fromExploreNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "explore");
    }

    public static UIEvent fromLikesNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "collection_likes");
    }

    public static UIEvent fromPlaylistsNav() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "collection_playlists");
    }

    public static UIEvent fromSearchAction() {
        return new UIEvent(Kind.NAVIGATION).putAttribute("page", "search");
    }

    private static String getPlayableType(Playable playable) {
        return (playable instanceof PublicApiTrack ? "track" : "playlist");
    }

    public UIEvent(Kind kind) {
        this.kind = kind;
        attributes = new HashMap<String, String>();
    }

    public Kind getKind() {
        return kind;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return String.format("UI Event with type id %s and %s", kind, attributes.toString());
    }

    private UIEvent putAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }
}
