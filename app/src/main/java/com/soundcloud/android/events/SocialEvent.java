package com.soundcloud.android.events;


public class SocialEvent {

    public static final int TYPE_FOLLOW = 0;
    public static final int TYPE_LIKE = 1;
    public static final int TYPE_REPOST = 2;
    public static final int TYPE_ADD_TO_PLAYLIST = 3;
    public static final int TYPE_COMMENT = 4;
    public static final int TYPE_SHARE = 5;

    private int mType;
    private Attributes mAttributes;


    public static SocialEvent fromFollow(String screenTag, int userId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.userId = userId;
        return new SocialEvent(TYPE_FOLLOW, attributes);
    }

    public static SocialEvent fromLike(String screenTag, String resource, int resourceId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = resource;
        attributes.resourceId = resourceId;
        return new SocialEvent(TYPE_LIKE, attributes);
    }

    public static SocialEvent fromRepost(String screenTag, String resource, int resourceId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = resource;
        attributes.resourceId = resourceId;
        return new SocialEvent(TYPE_REPOST, attributes);
    }

    public static SocialEvent fromAddToPlaylist(String screenTag, boolean isNewPlaylist, int trackId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.isNewPlaylist = isNewPlaylist;
        attributes.trackId = trackId;
        return new SocialEvent(TYPE_ADD_TO_PLAYLIST, attributes);
    }

    public static SocialEvent fromComment(String screenTag, int trackId) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.trackId = trackId;
        return new SocialEvent(TYPE_COMMENT, attributes);
    }

    public static SocialEvent fromShare(String screenTag, String resource, int resourceId, String sharedTo) {
        Attributes attributes = new Attributes();
        attributes.screenTag = screenTag;
        attributes.resource = resource;
        attributes.resourceId = resourceId;
        attributes.sharedTo = sharedTo;
        return new SocialEvent(TYPE_SHARE, attributes);
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

    public static class Attributes {
        public String screenTag;
        public int userId;
        public String resource;
        public int resourceId;
        public int trackId;
        public boolean isNewPlaylist;
        public String sharedTo;
    }
}
