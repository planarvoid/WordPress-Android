package com.soundcloud.android.main;

import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;

import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

public enum Screen {

    UNKNOWN(0, "unknown"),

    // auth
    TOUR(1, "tour:main"),
    AUTH_LOG_IN(2, "auth:log_in_prompt"),
    AUTH_SIGN_UP(3, "auth:sign_up_prompt"),
    AUTH_TERMS(4, "auth:accept_terms"),
    AUTH_USER_DETAILS(5, "auth:user_details"),
    AUTH_FORGOT_PASSWORD(6, "auth:forgot_password"),

    VERIFY_AGE(7, "verify_age"),

    SIDE_MENU_DRAWER(8, "drawer"),
    STREAM(9, "stream:main"),

    // ads
    VIDEO_FULLSCREEN(10, "ads:video"),
    PRESTITIAL(11, "ads:display"),

    // collection
    COLLECTIONS(12, "collection:main"),
    LIKES(13, "collection:likes"),
    PLAYLISTS(14, "collection:playlists"),
    OFFLINE_ONBOARDING(15, "collection:offline_onboarding"),
    OFFLINE_OFFBOARDING(16, "collection:offline_offboarding"),
    PLAY_HISTORY(17, "collection:history"),
    RECENTLY_PLAYED(18, "collection:recently_played"),
    LIKED_STATIONS(19, "collection:stations"),

    // onboarding
    ONBOARDING_MAIN(20, "onboarding:main"),
    ONBOARDING_GENRE(21, "onboarding:genre"),
    ONBOARDING_FACEBOOK(22, "onboarding:facebook"),

    // your own profile
    YOUR_POSTS(23, "user:posts"),
    YOUR_INFO(24, "user:info"),
    YOUR_PLAYLISTS(25, "user:playlists"),
    YOUR_LIKES(26, "user:likes"),
    YOUR_FOLLOWINGS(27, "user:followings"),
    YOUR_FOLLOWERS(28, "user:followers"),
    YOUR_MAIN(29, "user:main"),

    // other user's profiles
    USER_HEADER(30, "users:header"),
    USER_POSTS(31, "users:posts"),
    USER_INFO(32, "users:info"),
    USER_FOLLOWINGS(33, "users:followings"),
    USER_FOLLOWERS(34, "users:followers"),
    USERS_REPOSTS(35, "users:reposts"),
    USER_TRACKS(36, "users:tracks"),
    USER_ALBUMS(37, "users:albums"),
    USER_LIKES(38, "users:likes"),
    USER_PLAYLISTS(39, "users:playlists"),
    USER_MAIN(40, "users:main"),

    // player screens
    PLAYER_MAIN(41, "tracks:main"),
    PLAYER_INFO(42, "tracks:info"),
    PLAYER_LIKES(43, "tracks:likes"),
    PLAYER_REPOSTS(44, "tracks:reposts"),
    PLAYER_COMMENTS(45, "tracks:comments"),

    // play queue
    PLAY_QUEUE(46, "play_queue:main"),

    // discover / new home
    DISCOVER(47, "discovery:main"),

    SYSTEM_PLAYLIST(48, "systemplaylists:main"),

    // search
    SEARCH_MAIN(49, "search:main"),
    SEARCH_EVERYTHING(50, "search:everything"),
    SEARCH_TRACKS(51, "search:tracks"),
    SEARCH_TOP_RESULTS(52, "search:top_results"),
    SEARCH_ALBUMS(53, "search:albums"),
    SEARCH_PLAYLISTS(54, "search:playlists"),
    SEARCH_USERS(55, "search:people"),
    SEARCH_PREMIUM_CONTENT(56, "search:high_tier"),
    SEARCH_SUGGESTIONS(57, "search:suggestions"),

    // creators
    RECORD_MAIN(66, "record:main"),
    RECORD_EDIT(67, "record:edit"),
    RECORD_UPLOAD(68, "record:share"),
    RECORD_PROGRESS(69, "record:progress"),
    DEEPLINK_UPLOAD(70, "deeplink:share"),

    // playlists
    PLAYLIST_DETAILS(72, "playlists:main"),
    PLAYLIST_LIKES(73, "playlists:likes"),
    PLAYLIST_REPOSTS(74, "playlists:reposts"),

    // misc
    ACTIVITIES(75, "activity:main"),
    MORE(76, "more:main"),

    // settings
    SETTINGS_MAIN(77, "settings:main"),
    SETTINGS_NOTIFICATIONS(78, "settings:notification_settings"),
    SETTINGS_ACCOUNT(79, "settings:account_sync_settings"),
    SETTINGS_OFFLINE(80, "settings:offline_sync_settings"),
    SETTINGS_OFFLINE_STORAGE_LOCATION(81, "settings:offline_storage_location"),
    SETTINGS_OFFLINE_STORAGE_LOCATION_CONFIRM(82, "settings:offline_storage_location_confirm"),
    SETTINGS_LEGAL(83, "settings:legal"),
    SETTINGS_LICENSES(84, "settings:licenses"),
    SETTINGS_AUTOMATIC_SYNC_ONBOARDING(85, "settings:automatic_sync_onboarding"),

    WIDGET(86, "widget"),
    SUGGESTED_USERS(87, "suggested_users"),
    VOICE_COMMAND(88, "voice:command"), // context when we play from voice search
    DEEPLINK(89, "deeplink"), // context provided when we intercept a track
    NOTIFICATION(90, "notification"),

    // Stations
    STATIONS_SHOW_ALL(91, "stations:show_all"),
    STATIONS_INFO(92, "stations:main"),

    // PAYMENT
    CONVERSION(93, "consumer-premium:main"),
    PLAN_CHOICE(94, "consumer-premium:plans"),
    CHECKOUT(95, "consumer-premium:checkout");

    private static final String ORDINAL_EXTRA = "ScreenOrdinal";

    private final String trackingTag;
    private final int trackingOrdinal;

    Screen(int trackingOrdinal, String trackingTag) {
        this.trackingOrdinal = trackingOrdinal;
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

    public static Screen fromTrackingOrdinal(int trackingOrdinal) {
        // TODO should we rewrite this in a more efficient way that doesn't do a lookup every time?
        return Iterables.find(Lists.newArrayList(values()), input -> input.trackingOrdinal == trackingOrdinal, Screen.UNKNOWN);
    }

    public int trackingOrdinal() {
        return trackingOrdinal;
    }

    public void addToBundle(Bundle bundle) {
        bundle.putInt(Screen.ORDINAL_EXTRA, trackingOrdinal());
    }

    public void addToIntent(Intent intent) {
        intent.putExtra(Screen.ORDINAL_EXTRA, trackingOrdinal());
    }

    public static Screen fromIntent(Intent intent) {
        return fromTrackingOrdinal(intent.getIntExtra(Screen.ORDINAL_EXTRA, 0));
    }

    public static Screen fromBundle(Bundle bundle) {
        return fromTrackingOrdinal(bundle.getInt(Screen.ORDINAL_EXTRA, 0));
    }

    public static boolean hasScreen(Intent intent) {
        return intent.hasExtra(Screen.ORDINAL_EXTRA);
    }
}
