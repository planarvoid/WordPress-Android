package com.soundcloud.android.analytics.localytics;

public interface LocalyticsEvents {

    String LISTEN = "Listen";

    interface UI {
        String FOLLOW = "Follow";
        String UNFOLLOW = "Unfollow";
        String LIKE = "Like";
        String UNLIKE = "Unlike";
        String REPOST = "Repost";
        String UNREPOST = "Unrepost";
        String ADD_TO_PLAYLIST = "Add to playlist";
        String COMMENT = "Comment";
        String SHARE = "Share";
        String SHUFFLE_LIKES = "Shuffle likes";
    }

    interface Onboarding {
        String AUTH_PROMPT = "Auth prompt";
        String AUTH_CREDENTIALS = "Auth credentials";
        String CONFIRM_TERMS = "Confirm terms";
        String AUTH_COMPLETE = "Auth complete";
        String SAVE_USER_INFO = "Save user info";
        String SKIP_USER_INFO = "Skip user info";
        String ONBOARDING_COMPLETE = "Onboarding complete";
    }

}
