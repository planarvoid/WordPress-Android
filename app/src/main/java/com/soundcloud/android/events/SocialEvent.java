package com.soundcloud.android.events;


import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SocialEvent {

    public static final int TYPE_FOLLOW = 0;
    public static final int TYPE_UNFOLLOW = 1;
    public static final int TYPE_LIKE = 2;
    public static final int TYPE_UNLIKE = 3;
    public static final int TYPE_REPOST = 4;
    public static final int TYPE_UNREPOST = 5;
    public static final int TYPE_ADD_TO_PLAYLIST = 6;
    public static final int TYPE_COMMENT = 7;
    public static final int TYPE_SHARE = 8;

    private int mType;
    private Map<String, String> mAttributes;


    public static SocialEvent fromFollow(String screenTag, long userId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("user_id", String.valueOf(userId));
        return new SocialEvent(TYPE_FOLLOW, attributes);
    }

    public static SocialEvent fromUnfollow(String screenTag, long userId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("user_id", String.valueOf(userId));
        return new SocialEvent(TYPE_UNFOLLOW, attributes);
    }

    public static SocialEvent fromLike(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(TYPE_LIKE, attributes);
    }

    public static SocialEvent fromUnlike(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(TYPE_UNLIKE, attributes);
    }

    public static SocialEvent fromRepost(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(TYPE_REPOST, attributes);
    }

    public static SocialEvent fromUnrepost(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(TYPE_UNREPOST, attributes);
    }

    public static SocialEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("is_new_playlist", isNewPlaylist ? "yes" : "no");
        attributes.put("track_id", String.valueOf(trackId));
        return new SocialEvent(TYPE_ADD_TO_PLAYLIST, attributes);
    }

    public static SocialEvent fromComment(String screenTag, long trackId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("track_id", String.valueOf(trackId));
        return new SocialEvent(TYPE_COMMENT, attributes);
    }

    public static SocialEvent fromShare(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(TYPE_SHARE, attributes);
    }

    private static String getPlayableType(Playable playable) {
        return (playable instanceof Track ? "track" : "playlist");
    }

    private SocialEvent(int type, Map<String, String> attributes) {
        mType = type;
        mAttributes = attributes;
    }

    public int getType() {
        return mType;
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return  String.format("Social Event with type id %s and %s",  getType(), getAttributes().toString());
    }
}
