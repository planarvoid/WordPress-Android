package com.soundcloud.android.analytics.localytics;

final class LocalyticsEvents {

    static final String LISTEN = "Listen";
    static final String PAGEVIEW = "Pageview";
    static final String PLAY_CONTROLS = "Play Controls";
    static final String AD_FAILED_TO_BUFFER = "Ad failed to buffer";
    static final String BUFFER_UNDERRUN = "Buffer Underrun";
    static final String SKIPPY_PLAY = "Skippy Play";
    static final String SKIPPY_INITILIAZATION_ERROR = "Skippy Init Error";
    static final String SKIPPY_INITILIAZATION_SUCCESS = "Skippy Init Success";
    static final String AD_DEBUG = "Ad Debug";

    static final class UI {
        static final String FOLLOW = "Follow";
        static final String UNFOLLOW = "Unfollow";
        static final String LIKE = "Like";
        static final String UNLIKE = "Unlike";
        static final String REPOST = "Repost";
        static final String UNREPOST = "Unrepost";
        static final String ADD_TO_PLAYLIST = "Add to playlist";
        static final String COMMENT = "Comment";
        static final String SHARE = "Share";
        static final String SHUFFLE_LIKES = "Shuffle likes";
        static final String NAVIGATION = "Navigation";
        static final String PLAYER_CLOSE = "Player close";
        static final String PLAYER_OPEN = "Player open";
    }

    static final class Onboarding {
        static final String AUTH_PROMPT = "Auth prompt";
        static final String AUTH_CREDENTIALS = "Auth credentials";
        static final String CONFIRM_TERMS = "Confirm terms";
        static final String AUTH_COMPLETE = "Auth complete";
        static final String USER_INFO = "User info";
        static final String ONBOARDING_COMPLETE = "Onboarding complete";
        static final String EMAIL_MARKETING = "Email marketing";
        static final String SIGNUP_ERROR = "Signup Error";
    }

    static final class Search {
        static final String SEARCH_SUGGESTION = "Search suggestion";
        static final String SEARCH_SUBMIT = "Search submit";
        static final String SEARCH_RESULTS = "Search results";
    }

}
