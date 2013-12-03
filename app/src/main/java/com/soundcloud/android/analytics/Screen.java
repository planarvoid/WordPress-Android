package com.soundcloud.android.analytics;

import java.util.Locale;

public enum Screen {

    // core screens
    SIDE_MENU_STREAM("stream:main"),
    SIDE_MENU_LIKES("collection:likes"),
    SIDE_MENU_PLAYLISTS("collection:playlists"),

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

    private String mTag;
}
