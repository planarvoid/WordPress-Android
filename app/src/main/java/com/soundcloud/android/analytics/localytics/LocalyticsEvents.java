package com.soundcloud.android.analytics.localytics;

public interface LocalyticsEvents {

    String LISTEN = "Listen";

    interface Social {
        String FOLLOW = "Follow";
        String LIKE = "Like";
        String REPOST = "Repost";
        String ADD_TO_PLAYLIST = "Add to playlist";
        String COMMENT = "Comment";
        String SHARE = "Share";
    }

}
