package com.soundcloud.android.analytics.localytics;

public interface LocalyticsEvents {

    String LISTEN = "Listen";

    interface Social {
        String FOLLOW = "Follow";
        String UNFOLLOW = "Unfollow";
        String LIKE = "Like";
        String UNLIKE = "Unlike";
        String REPOST = "Repost";
        String UNREPOST = "Unrepost";
        String ADD_TO_PLAYLIST = "Add to playlist";
        String COMMENT = "Comment";
        String SHARE = "Share";
    }

}
