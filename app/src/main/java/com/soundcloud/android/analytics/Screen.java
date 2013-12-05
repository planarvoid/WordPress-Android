package com.soundcloud.android.analytics;

import android.net.Uri;

import java.util.Locale;

public enum Screen {

    // core screens
    SIDE_MENU_STREAM("stream:main"),
    SIDE_MENU_LIKES("collection:likes"),
    SIDE_MENU_PLAYLISTS("collection:playlists"),

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
    // explore screens
    EXPLORE_GENRES("explore:genres"),
    EXPLORE_TRENDING_MUSIC("explore:trending_music"),
    EXPLORE_TRENDING_AUDIO("explore:trending_audio"),
    EXPLORE_AUDIO_GENRE("explore:genres:audio"),
    EXPLORE_MUSIC_GENRE("explore:genres:music");

    private Screen(String trackingTag) {
        mTag = trackingTag;
    }

    public String get() {
        return mTag;
    }

    public String get(String postfix) {
        return new StringBuilder(mTag).append(":").append(postfix.toLowerCase(Locale.US).replaceAll(" ", "_")).toString();
    }

    public Uri toUri() {
        return Uri.parse(mTag);
    }

    private String mTag;
}
