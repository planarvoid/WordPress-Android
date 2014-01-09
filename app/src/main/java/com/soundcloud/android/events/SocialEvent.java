package com.soundcloud.android.events;


import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.NotNull;

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
    private Attributes mAttributes;


    public static SocialEvent fromFollow(String screenTag, long userId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.userId = userId;
        return new SocialEvent(TYPE_FOLLOW, attributes);
    }

    public static SocialEvent fromUnfollow(String screenTag, long userId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.userId = userId;
        return new SocialEvent(TYPE_UNFOLLOW, attributes);
    }

    public static SocialEvent fromLike(String screenTag, @NotNull Playable playable) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = getPlayableType(playable);
        attributes.resourceId = playable.getId();
        return new SocialEvent(TYPE_LIKE, attributes);
    }

    public static SocialEvent fromUnlike(String screenTag, @NotNull Playable playable) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = getPlayableType(playable);
        attributes.resourceId = playable.getId();
        return new SocialEvent(TYPE_UNLIKE, attributes);
    }

    public static SocialEvent fromRepost(String screenTag, @NotNull Playable playable) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = getPlayableType(playable);
        attributes.resourceId = playable.getId();
        return new SocialEvent(TYPE_REPOST, attributes);
    }

    public static SocialEvent fromUnrepost(String screenTag, @NotNull Playable playable) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = getPlayableType(playable);
        attributes.resourceId = playable.getId();
        return new SocialEvent(TYPE_UNREPOST, attributes);
    }

    public static SocialEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, long trackId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.isNewPlaylist = isNewPlaylist;
        attributes.trackId = trackId;
        return new SocialEvent(TYPE_ADD_TO_PLAYLIST, attributes);
    }

    public static SocialEvent fromComment(String screenTag, long trackId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.trackId = trackId;
        return new SocialEvent(TYPE_COMMENT, attributes);
    }

    public static SocialEvent fromShare(String screenTag, @NotNull Playable playable) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = getPlayableType(playable);
        attributes.resourceId = playable.getId();
        return new SocialEvent(TYPE_SHARE, attributes);
    }

    private static String getPlayableType(Playable playable) {
        return (playable instanceof Track ? "track" : "playlist");
    }

    private SocialEvent(int type, Attributes attributes) {
        mType = type;
        mAttributes = attributes;
    }

    public int getType() {
        return mType;
    }

    public Attributes getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return  String.format("Social Event with type id %s and %s",  getType(), getAttributes().toString());
    }

    public static class Attributes {
        public String screenTag;
        public long userId;
        public String resource;
        public long resourceId;
        public long trackId;
        public boolean isNewPlaylist;

        @Override
        public String toString() {
            return String.format("screenTag: %s", screenTag);
        }
    }
}
