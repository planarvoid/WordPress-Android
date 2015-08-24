package com.soundcloud.android.analytics.localytics;

final class LocalyticsEvents {

    static final String LISTEN = "Listen";
    static final String PAGEVIEW = "Pageview";
    static final String PLAY_CONTROLS = "Play Controls";
    static final String AD_FAILED_TO_BUFFER = "Ad failed to buffer";

    static final class UI {
        static final String FOLLOW = "Follow";
        static final String LIKE = "Like";
        static final String UNLIKE = "Unlike";
        static final String REPOST = "Repost";
        static final String ADD_TO_PLAYLIST = "Add to playlist";
        static final String COMMENT = "Comment";
        static final String SHARE = "Share";
    }

    static final class Onboarding {
        static final String AUTH_PROMPT = "Auth prompt";
        static final String AUTH_CREDENTIALS = "Auth credentials";
        static final String AUTH_COMPLETE = "Auth complete";
        static final String ONBOARDING_COMPLETE = "Onboarding complete";
        static final String SIGNUP_ERROR = "Signup Error";
    }

}
