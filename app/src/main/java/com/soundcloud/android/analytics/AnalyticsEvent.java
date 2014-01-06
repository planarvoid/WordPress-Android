package com.soundcloud.android.analytics;


public class AnalyticsEvent {

    public static final int TYPE_FOLLOW = 0;
    public static final int TYPE_LIKE = 1;
    public static final int TYPE_REPOST = 2;
    public static final int TYPE_ADD_TO_PLAYLIST = 3;
    public static final int TYPE_COMMENT = 4;
    public static final int TYPE_SHARE = 5;

    private int mType;
    private Attributes mAttributes;


    public static AnalyticsEvent createFollow(String screenTag, int userId) {
        Attributes attributes = new Attributes();
        attributes.userId = userId;
        return new AnalyticsEvent(TYPE_FOLLOW, attributes);
    }

    public static AnalyticsEvent createLike(String screenTag, String resource, int resourceId) {
        Attributes attributes = new Attributes();
        attributes.resource = resource;
        attributes.resourceId = resourceId;
        return new AnalyticsEvent(TYPE_LIKE, attributes);
    }

    public static AnalyticsEvent createRepost(String screenTag, String resource, int resourceId) {
        Attributes attributes = new Attributes();
        attributes.resource = resource;
        attributes.resourceId = resourceId;
        return new AnalyticsEvent(TYPE_REPOST, attributes);
    }

    public static AnalyticsEvent createAddToPlaylist(String screenTag, boolean isNewPlaylist, int trackId) {
        Attributes attributes = new Attributes();
        attributes.isNewPlaylist = isNewPlaylist;
        attributes.trackId = trackId;
        return new AnalyticsEvent(TYPE_ADD_TO_PLAYLIST, attributes);
    }

    public static AnalyticsEvent createComment(String screenTag, int trackId) {
        Attributes attributes = new Attributes();
        attributes.trackId = trackId;
        return new AnalyticsEvent(TYPE_COMMENT, attributes);
    }

    public static AnalyticsEvent createComment(String screenTag, String resource, int resourceId, String sharedTo) {
        Attributes attributes = new Attributes();
        attributes.resource = resource;
        attributes.resourceId = resourceId;
        attributes.sharedTo = sharedTo;
        return new AnalyticsEvent(TYPE_SHARE, attributes);
    }

    private AnalyticsEvent(int type, Attributes attributes) {
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
        return "";
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
