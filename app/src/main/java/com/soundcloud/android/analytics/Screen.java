package com.soundcloud.android.analytics;

import com.google.common.collect.Maps;
import com.soundcloud.android.Actions;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;
import java.util.Map;

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
    SIDE_MENU_STREAM("stream:main", Actions.STREAM),
    SIDE_MENU_LIKES("collection:likes", Actions.LIKES),
    SIDE_MENU_PLAYLISTS("collection:playlists"),

    // onboarding
    ONBOARDING_MAIN("onboarding:main"),
    ONBOARDING_GENRE("onboarding:genre"),
    ONBOARDING_FACEBOOK("onboarding:facebook"),

    // your own profile
    YOUR_POSTS("you:posts", Actions.YOUR_SOUNDS),
    YOUR_INFO("you:info"),
    YOUR_PLAYLISTS("you:playlists"),
    YOUR_LIKES("you:likes", Actions.YOUR_LIKES),
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

    // creators
    RECORD_MAIN("record:main"),
    RECORD_EDIT("record:edit"),
    RECORD_UPLOAD("record:share"),

    // playlists
    PLAYLIST_DETAILS("playlists:main"),
    PLAYLIST_LIKES("playlists:likes"),
    PLAYLIST_REPOSTS("playlists:reposts"),

    // misc
    WHO_TO_FOLLOW("who_to_follow:main"),
    ACTIVITIES("activity:main"),
    SEARCH_BY_TAG("tags:main"),

    // explore screens
    EXPLORE_GENRES("explore:genres"),
    EXPLORE_TRENDING_MUSIC("explore:trending_music"),
    EXPLORE_TRENDING_AUDIO("explore:trending_audio"),
    EXPLORE_AUDIO_GENRE("explore:audio"),
    EXPLORE_MUSIC_GENRE("explore:music"),

    // settings
    SETTINGS_MAIN("settings:main"),
    SETTINGS_CHANGE_LOG("settings:change_log"),
    SETTINGS_NOTIFICATIONS("settings:notification_settings"),
    SETTINGS_ACCOUNT("settings:account_sync_settings"),

    // context provided when we intercept a track
    DEEPLINK("deeplink");

    private static final String ORDINAL_EXTRA = "ScreenOrdinal";
    private static final String EXPLORE_PREFIX = "explore";

    private static final Map<String, Screen> SCREEN_TAG_MAP = Maps.newHashMap();

    static {
        for (Screen screen : Screen.values()) {
            SCREEN_TAG_MAP.put(screen.get(), screen);
        }
    }

    private Screen(String trackingTag, String upAction) {
        this.trackingTag = trackingTag;
        this.upAction = upAction;
    }

    private Screen(String trackingTag) {
        this(trackingTag, null);
    }

    public String get() {
        return trackingTag;
    }

    public String get(String postfix) {
        return new StringBuilder(trackingTag).append(":").append(postfix.toLowerCase(Locale.US).replaceAll(" ", "_")).toString();
    }

    public void addToBundle(Bundle bundle) {
        bundle.putInt(Screen.ORDINAL_EXTRA, ordinal());
    }

    public void addToIntent(Intent intent) {
        intent.putExtra(Screen.ORDINAL_EXTRA, ordinal());
    }

    private final String trackingTag;
    private final String upAction;

    public static Screen fromIntent(Intent intent) {
        return values()[intent.getIntExtra(Screen.ORDINAL_EXTRA, -1)];
    }

    public static Screen fromIntent(Intent intent, Screen defaultScreen) {
        return intent.hasExtra(Screen.ORDINAL_EXTRA) ? values()[intent.getIntExtra(Screen.ORDINAL_EXTRA, -1)] : defaultScreen;
    }

    public static Screen fromBundle(Bundle bundle) {
        return values()[bundle.getInt(Screen.ORDINAL_EXTRA, -1)];
    }

    public static Screen fromScreenTag(String screenTag) {
        if (SCREEN_TAG_MAP.containsKey(screenTag)) {
            return SCREEN_TAG_MAP.get(screenTag);
        }
        throw new IllegalArgumentException("Unrecognized screenTag: " + screenTag);
    }

    public static Intent getUpDestinationFromScreenTag(@NotNull String screenTag) {
        if (screenTag.startsWith(EXPLORE_PREFIX)) {
            return new Intent(Actions.EXPLORE).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            final String upAction = fromScreenTag(screenTag).upAction;
            if (ScTextUtils.isNotBlank(upAction)) {
                return new Intent(upAction).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
        }
        return null;
    }


}
