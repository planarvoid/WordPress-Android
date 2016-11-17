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

    VERIFY_AGE("verify_age"),

    SIDE_MENU_DRAWER("drawer"),
    STREAM("stream:main"),

    // collection
    COLLECTIONS("collection:main"),
    LIKES("collection:likes"),
    PLAYLISTS("collection:playlists"),
    OFFLINE_ONBOARDING("collection:offline_onboarding"),
    OFFLINE_OFFBOARDING("collection:offline_offboarding"),
    PLAY_HISTORY("collection:history"),
    RECENTLY_PLAYED("collection:recently_played"),

    // onboarding
    ONBOARDING_MAIN("onboarding:main"),
    ONBOARDING_GENRE("onboarding:genre"),
    ONBOARDING_FACEBOOK("onboarding:facebook"),

    // your own profile
    YOUR_POSTS("user:posts"),
    YOUR_INFO("user:info"),
    YOUR_PLAYLISTS("user:playlists"),
    YOUR_LIKES("user:likes"),
    YOUR_FOLLOWINGS("user:followings"),
    YOUR_FOLLOWERS("user:followers"),
    YOUR_MAIN("user:main"),

    // other user's profiles
    USER_HEADER("users:header"),
    USER_POSTS("users:posts"),
    USER_INFO("users:info"),
    USER_FOLLOWINGS("users:followings"),
    USER_FOLLOWERS("users:followers"),
    USERS_REPOSTS("users:reposts"),
    USER_TRACKS("users:tracks"),
    USER_ALBUMS("users:albums"),
    USER_LIKES("users:likes"),
    USER_PLAYLISTS("users:playlists"),
    USER_MAIN("users:main"),

    // player screens
    PLAYER_MAIN("tracks:main"),
    PLAYER_INFO("tracks:info"),
    PLAYER_LIKES("tracks:likes"),
    PLAYER_REPOSTS("tracks:reposts"),
    PLAYER_COMMENTS("tracks:comments"),

    // play queue
    PLAY_QUEUE("play_queue:main"),

    // search
    SEARCH_MAIN("search:main"),
    SEARCH_EVERYTHING("search:everything"),
    SEARCH_TRACKS("search:tracks"),
    SEARCH_ALBUMS("search:albums"),
    SEARCH_PLAYLISTS("search:playlists"),
    SEARCH_USERS("search:people"),
    SEARCH_PREMIUM_CONTENT("search:high_tier"),
    SEARCH_SUGGESTIONS("search:suggestions"),
    SEARCH_PLAYLIST_DISCO("search:tags"),

    // recommendations
    RECOMMENDATIONS_MAIN("personal-recommended:main"),

    // charts
    MUSIC_GENRES("charts:music_genres"),
    AUDIO_GENRES("charts:audio_genres"),
    MUSIC_TRENDING("charts:music_trending:%s"),
    AUDIO_TRENDING("charts:audio_trending:%s"),
    MUSIC_TOP_50("charts:music_top_50:%s"),
    AUDIO_TOP_50("charts:audio_top_50:%s"),

    // creators
    RECORD_MAIN("record:main"),
    RECORD_EDIT("record:edit"),
    RECORD_UPLOAD("record:share"),
    RECORD_PROGRESS("record:progress"),
    DEEPLINK_UPLOAD("deeplink:share"),

    // playlists
    PLAYLIST_DETAILS("playlists:main"),
    PLAYLIST_LIKES("playlists:likes"),
    PLAYLIST_REPOSTS("playlists:reposts"),

    // misc
    ACTIVITIES("activity:main"),
    MORE("more:main"),

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
    SETTINGS_LEGAL("settings:legal"),
    SETTINGS_LICENSES("settings:licenses"),
    SETTINGS_AUTOMATIC_SYNC_ONBOARDING("settings:automatic_sync_onboarding"),

    WIDGET("widget"),
    SUGGESTED_USERS("suggested_users"),
    VOICE_COMMAND("voice:command"), // context when we play from voice search
    DEEPLINK("deeplink"), // context provided when we intercept a track
    NOTIFICATION("notification"),

    // Stations
    STATIONS_SHOW_ALL("stations:show_all"),
    STATIONS_LIKED("stations:liked_stations"),
    STATIONS_INFO("stations:main"),

    // PAYMENT
    CONVERSION("consumer-premium:main"),
    CHECKOUT("consumer-premium:checkout");

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

    public static Screen fromTag(String tag) {
        for (Screen screen : values()) {
            if (screen.trackingTag.equals(tag)) {
                return screen;
            }
        }
        return Screen.UNKNOWN;
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
