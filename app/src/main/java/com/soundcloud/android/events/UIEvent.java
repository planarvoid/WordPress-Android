package com.soundcloud.android.events;


import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UIEvent implements Event {

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

    private final int mKind;
    private final Map<String, String> mAttributes;

    public static UIEvent fromToggleFollow(boolean isFollow, String screenTag, long userId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("user_id", String.valueOf(userId));
        return new UIEvent(isFollow ? FOLLOW : UNFOLLOW, attributes);
    }

    public static UIEvent fromToggleLike(boolean isLike, String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new UIEvent(isLike ? LIKE : UNLIKE, attributes);
    }

    public static UIEvent fromRepost(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new UIEvent(REPOST, attributes);
    }

    public static UIEvent fromUnrepost(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new UIEvent(UNREPOST, attributes);
    }

    public static UIEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("is_new_playlist", isNewPlaylist ? "yes" : "no");
        attributes.put("track_id", String.valueOf(trackId));
        return new UIEvent(ADD_TO_PLAYLIST, attributes);
    }

    public static UIEvent fromComment(String screenTag, long trackId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("track_id", String.valueOf(trackId));
        return new UIEvent(COMMENT, attributes);
    }

    public static UIEvent fromShare(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new UIEvent(SHARE, attributes);
    }

    public static UIEvent fromShuffleMyLikes() {
        return new UIEvent(SHUFFLE_LIKES);
    }

    private static String getPlayableType(Playable playable) {
        return (playable instanceof Track ? "track" : "playlist");
    }

    private UIEvent(int kind, Map<String, String> attributes) {
        mKind = kind;
        mAttributes = attributes;
    }

    public UIEvent(int kind) {
        this(kind, Collections.<String,String>emptyMap());
    }

    @Override
    public int getKind() {
        return mKind;
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return  String.format("UI Event with type id %s and %s", mKind, mAttributes.toString());
    }
}
