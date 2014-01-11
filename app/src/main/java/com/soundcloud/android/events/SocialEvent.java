package com.soundcloud.android.events;


import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class SocialEvent extends Event {

    public static final int FOLLOW = 0;
    public static final int UNFOLLOW = 1;
    public static final int LIKE = 2;
    public static final int UNLIKE = 3;
    public static final int REPOST = 4;
    public static final int UNREPOST = 5;
    public static final int ADD_TO_PLAYLIST = 6;
    public static final int COMMENT = 7;
    public static final int SHARE = 8;

    public static SocialEvent fromFollow(String screenTag, long userId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("user_id", String.valueOf(userId));
        return new SocialEvent(FOLLOW, attributes);
    }

    public static SocialEvent fromUnfollow(String screenTag, long userId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("user_id", String.valueOf(userId));
        return new SocialEvent(UNFOLLOW, attributes);
    }

    public static SocialEvent fromLike(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(LIKE, attributes);
    }

    public static SocialEvent fromUnlike(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(UNLIKE, attributes);
    }

    public static SocialEvent fromRepost(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(REPOST, attributes);
    }

    public static SocialEvent fromUnrepost(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(UNREPOST, attributes);
    }

    public static SocialEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("is_new_playlist", isNewPlaylist ? "yes" : "no");
        attributes.put("track_id", String.valueOf(trackId));
        return new SocialEvent(ADD_TO_PLAYLIST, attributes);
    }

    public static SocialEvent fromComment(String screenTag, long trackId) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("track_id", String.valueOf(trackId));
        return new SocialEvent(COMMENT, attributes);
    }

    public static SocialEvent fromShare(String screenTag, @NotNull Playable playable) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", screenTag);
        attributes.put("resource", getPlayableType(playable));
        attributes.put("resource_id", String.valueOf(playable.getId()));
        return new SocialEvent(SHARE, attributes);
    }

    private static String getPlayableType(Playable playable) {
        return (playable instanceof Track ? "track" : "playlist");
    }

    private SocialEvent(int type, Map<String, String> attributes) {
        super(type, attributes);
    }

    @Override
    public String toString() {
        return  String.format("Social Event with type id %s and %s", mKind, mAttributes.toString());
    }
}
