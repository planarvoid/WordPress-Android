package com.soundcloud.android.main;

import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

public enum Screen {

    UNKNOWN("unknown"),

    // auth
    TOUR("tour:main"),
    AUTH_LOG_IN("auth:log_in_prompt"),
    AUTH_SIGN_UP("auth:sign_up_prompt"),
    AUTH_TERMS("auth:accept_terms"),
    AUTH_USER_DETAILS("auth:user_details"),
    AUTH_FORGOT_PASSWORD("auth:forgot_password"),

    // core screens
    SIDE_MENU_DRAWER("drawer"),
    STREAM("stream:main"),
    SIDE_MENU_LIKES("collection:likes"),
    SIDE_MENU_PLAYLISTS("collection:playlists"),
    COLLECTIONS("collections"),

    // onboarding
    ONBOARDING_MAIN("onboarding:main"),
    ONBOARDING_GENRE("onboarding:genre"),
    ONBOARDING_FACEBOOK("onboarding:facebook"),

    // your own profile
    YOUR_POSTS("you:posts"),
    YOUR_INFO("you:info"),
    YOUR_PLAYLISTS("you:playlists"),
    YOUR_LIKES("you:likes"),
    YOUR_FOLLOWINGS("you:followings"),
    YOUR_FOLLOWERS("you:followers"),

    // other user's profiles
    USER_HEADER("users:header"),
    USER_POSTS("users:posts"),
    USER_INFO("users:info"),
    USER_PLAYLISTS("users:playlists"),
    USER_LIKES("users:likes"),
    USER_FOLLOWINGS("users:followings"),
    USER_FOLLOWERS("users:followers"),

    // player screens
    PLAYER_MAIN("tracks:main"),
    PLAYER_INFO("tracks:info"),
    PLAYER_LIKES("tracks:likes"),
    PLAYER_REPOSTS("tracks:reposts"),
    PLAYER_COMMENTS("tracks:comments"),

    // search
    SEARCH_MAIN("search:main"),
    SEARCH_EVERYTHING("search:everything"),
    SEARCH_TRACKS("search:tracks"),
    SEARCH_PLAYLISTS("search:playlists"),
    SEARCH_USERS("search:people"),
    SEARCH_SUGGESTIONS("search:suggestions"),
    SEARCH_PLAYLIST_DISCO("search:tags"),

    // recommendations
    RECOMMENDATIONS_MAIN("personal-recommended:main"),
    RECOMMENDATIONS_MORE("personal-recommended:more"),

    // creators
    RECORD_MAIN("record:main"),
    RECORD_EDIT("record:edit"),
    RECORD_UPLOAD("record:share"),
    RECORD_PROGRESS("record:progress"),

    // playlists
    PLAYLIST_DETAILS("playlists:main"),
    PLAYLIST_LIKES("playlists:likes"),
    PLAYLIST_REPOSTS("playlists:reposts"),

    // misc
    ACTIVITIES("activity:main"),
    YOU("you:main"),

    // explore screens
    EXPLORE_GENRES("explore:genres"),
    EXPLORE_TRENDING_MUSIC("explore:trending_music"),
    EXPLORE_TRENDING_AUDIO("explore:trending_audio"),
    EXPLORE_AUDIO_GENRE("explore:audio"),
    EXPLORE_MUSIC_GENRE("explore:music"),

    // settings
    SETTINGS_MAIN("settings:main"),
    SETTINGS_NOTIFICATIONS("settings:notification_settings"),
    SETTINGS_ACCOUNT("settings:account_sync_settings"),
    SETTINGS_OFFLINE("settings:offline_sync_settings"),

    WIDGET("widget"),
    SUBSCRIBE("subscribe"),
    SUBSCRIBE_SUCCESS("subscribe_success"),
    SUGGESTED_USERS("suggested_users"),
    VOICE_COMMAND("voice:command"), // context when we play from voice search
    DEEPLINK("deeplink"), // context provided when we intercept a track
    NOTIFICATION("notification"),

    // Stations
    STATIONS_HOME("stations:home"),
    STATIONS_SHOW_ALL("stations:show_all");

    private static final String ORDINAL_EXTRA = "ScreenOrdinal";

    private final String trackingTag;

    Screen(String trackingTag) {
        this.trackingTag = trackingTag;
    }

    public String get() {
        return trackingTag;
    }

    public String get(String postfix) {
        return trackingTag + ":" + postfix.toLowerCase(Locale.US).replaceAll(" ", "_");
    }

    public void addToBundle(Bundle bundle) {
        bundle.putInt(Screen.ORDINAL_EXTRA, ordinal());
    }

    public void addToIntent(Intent intent) {
        intent.putExtra(Screen.ORDINAL_EXTRA, ordinal());
    }

    public static Screen fromIntent(Intent intent) {
        return values()[intent.getIntExtra(Screen.ORDINAL_EXTRA, 0)];
    }

    public static Screen fromBundle(Bundle bundle) {
        return values()[bundle.getInt(Screen.ORDINAL_EXTRA, -1)];
    }

    public static boolean hasScreen(Intent intent) {
        return intent.hasExtra(Screen.ORDINAL_EXTRA);
    }
}
