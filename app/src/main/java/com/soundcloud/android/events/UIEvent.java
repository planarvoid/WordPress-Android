package com.soundcloud.android.events;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class UIEvent {

    public static final int FOLLOW = 0;
    public static final int UNFOLLOW = 1;
    public static final int LIKE = 2;
    public static final int UNLIKE = 3;
    public static final int REPOST = 4;
    public static final int UNREPOST = 5;
    public static final int ADD_TO_PLAYLIST = 6;
    public static final int COMMENT = 7;
    public static final int SHARE = 8;
    public static final int SHUFFLE_LIKES = 9;
    public static final int NAVIGATION = 10;

    private final int kind;
    private final Map<String, String> attributes;

    public static UIEvent fromToggleFollow(boolean isFollow, String screenTag, long userId) {
        return new UIEvent(isFollow ? FOLLOW : UNFOLLOW)
                .putAttribute("context", screenTag)
                .putAttribute("user_id", String.valueOf(userId));
    }

    public static UIEvent fromToggleLike(boolean isLike, String screenTag, @NotNull Playable playable) {
        return new UIEvent(isLike ? LIKE : UNLIKE)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromToggleRepost(boolean isRepost, String screenTag, @NotNull Playable playable) {
        return new UIEvent(isRepost ? REPOST : UNREPOST)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        return new UIEvent(ADD_TO_PLAYLIST)
                .putAttribute("context", screenTag)
                .putAttribute("is_new_playlist", isNewPlaylist ? "yes" : "no")
                .putAttribute("track_id", String.valueOf(trackId));
    }

    public static UIEvent fromComment(String screenTag, long trackId) {
        return new UIEvent(COMMENT)
                .putAttribute("context", screenTag)
                .putAttribute("track_id", String.valueOf(trackId));
    }

    public static UIEvent fromShare(String screenTag, @NotNull Playable playable) {
        return new UIEvent(SHARE)
                .putAttribute("context", screenTag)
                .putAttribute("resource", getPlayableType(playable))
                .putAttribute("resource_id", String.valueOf(playable.getId()));
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(SHUFFLE_LIKES);
    }

    public static UIEvent fromProfileNav() {
        return new UIEvent(NAVIGATION).putAttribute("page", "you");
    }

    public static UIEvent fromStreamNav() {
        return new UIEvent(NAVIGATION).putAttribute("page", "stream");
    }

    public static UIEvent fromExploreNav() {
        return new UIEvent(NAVIGATION).putAttribute("page", "explore");
    }

    public static UIEvent fromLikesNav() {
        return new UIEvent(NAVIGATION).putAttribute("page", "collection_likes");
    }

    public static UIEvent fromPlaylistsNav() {
        return new UIEvent(NAVIGATION).putAttribute("page", "collection_playlists");
    }

    public static UIEvent fromSearchAction() {
        return new UIEvent(NAVIGATION).putAttribute("page", "search");
    }

    private static String getPlayableType(Playable playable) {
        return (playable instanceof PublicApiTrack ? "track" : "playlist");
    }

    public UIEvent(int kind) {
        this.kind = kind;
        attributes = new HashMap<String, String>();
    }

    public int getKind() {
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
