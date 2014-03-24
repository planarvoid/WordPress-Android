package com.soundcloud.android.analytics.localytics;

public interface LocalyticsEvents {

    String LISTEN = "Listen";
    String PAGEVIEW = "Pageview";
    String PLAY_CONTROLS = "Play Controls";

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
        String NAVIGATION = "Navigation";
    }

    interface Onboarding {
        String AUTH_PROMPT = "Auth prompt";
        String AUTH_CREDENTIALS = "Auth credentials";
        String CONFIRM_TERMS = "Confirm terms";
        String AUTH_COMPLETE = "Auth complete";
        String USER_INFO = "User info";
        String ONBOARDING_COMPLETE = "Onboarding complete";
    }

    interface Search {
        String SEARCH_SUGGESTION = "Search suggestion";
        String SEARCH_SUBMIT = "Search submit";
        String SEARCH_RESULTS = "Search results";
    }

}
