package com.soundcloud.android.analytics;

import java.util.Locale;

public enum Screen {

    // auth
    TOUR("tour:main"),
    AUTH_LOG_IN("auth:log_in_prompt"),
    AUTH_SIGN_UP("auth:sign_up_prompt"),
    AUTH_TERMS("auth:accept_terms"),
    AUTH_USER_DETAILS("auth:user_details"),

    // core screens
    SIDE_MENU_STREAM("stream:main"),
    SIDE_MENU_LIKES("collection:likes"),
    SIDE_MENU_PLAYLISTS("collection:playlists"),

    // onboarding
    ONBOARDING_MAIN("onboarding:main"),

    // your own profile
    YOUR_POSTS("you:posts"),
    YOUR_INFO("you:info"),
    YOUR_PLAYLISTS("you:playlists"),
    YOUR_LIKES("you:likes"),
    YOUR_FOLLOWINGS("you:followings"),
    YOUR_FOLLOWERS("you:followers"),

    // other user's profiles
    USER_POSTS("users:posts"),
    USER_INFO("users:info"),
    USER_PLAYLISTS("users:playlists"),
    USER_LIKES("users:likes"),
    USER_FOLLOWINGS("users:followings"),
    USER_FOLLOWERS("users:followers"),

    // player screens
    PLAYER_MAIN("sounds:main"),
    PLAYER_INFO("sounds:info"),
    PLAYER_LIKES("sounds:likes"),
    PLAYER_REPOSTS("sounds:reposts"),
    PLAYER_COMMENTS("sounds:comments"),

    // search
    SEARCH_EVERYTHING("search:everything"),
    SEARCH_TRACKS("search:tracks"),
    SEARCH_PLAYLISTS("search:playlists"),
    SEARCH_USERS("search:people"),
    SEARCH_SUGGESTIONS("search:suggestions"),

    // creators
    RECORD_MAIN("record:main"),
    RECORD_EDIT("record:edit"),
    RECORD_UPLOAD("record:share"),

    // misc
    PLAYLIST_DETAILS("playlists:main"),
    WHO_TO_FOLLOW("who_to_follow:main"),
    ACTIVITIES("activity:main"),
    SEARCH_BY_TAG("tags:main"),

    // explore screens
    EXPLORE_GENRES("explore:genres"),
    EXPLORE_TRENDING_MUSIC("explore:trending_music"),
    EXPLORE_TRENDING_AUDIO("explore:trending_audio"),
    EXPLORE_AUDIO_GENRE("explore:genres:audio"),
    EXPLORE_MUSIC_GENRE("explore:genres:music"),

    // settings
    SETTINGS_MAIN("settings:main"),
    SETTINGS_CHANGE_LOG("settings:change_log"),
    SETTINGS_NOTIFICATIONS("settings:notification_settings"),
    SETTINGS_ACCOUNT("settings:account_sync_settings");

    private Screen(String trackingTag) {
        mTag = trackingTag;
    }

    public String get() {
        return mTag;
    }

    public String get(String postfix) {
        return new StringBuilder(mTag).append(":").append(postfix.toLowerCase(Locale.US).replaceAll(" ", "_")).toString();
    }

    private String mTag;
}
