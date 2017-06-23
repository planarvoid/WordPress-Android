package com.soundcloud.android.analytics.appboy;

enum AppboyAttributeName {

    CREATOR_DISPLAY_NAME("creator_display_name"),
    CREATOR_URN("creator_urn"),
    PLAYABLE_TITLE("playable_title"),
    PLAYABLE_URN("playable_urn"),
    PLAYABLE_TYPE("playable_type"),
    PLAYLIST_TITLE("playlist_title"),
    PLAYLIST_URN("playlist_urn"),
    CATEGORY("category"),
    GENRE("genre");

    private final String appBoyKey;

    AppboyAttributeName(String appBoyKey) {
        this.appBoyKey = appBoyKey;
    }

    String getAppBoyKey() {
        return appBoyKey;
    }

}
