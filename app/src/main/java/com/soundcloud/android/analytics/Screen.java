package com.soundcloud.android.analytics;

public enum Screen {

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
        return new StringBuilder(mTag).append(":").append(postfix.toLowerCase().replaceAll(" ", "_")).toString();
    }

    private String mTag;
}
