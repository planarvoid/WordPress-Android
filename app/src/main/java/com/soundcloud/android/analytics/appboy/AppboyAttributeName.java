package com.soundcloud.android.analytics.appboy;


import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.ScreenEvent;

public enum AppboyAttributeName {
    CREATOR_DISPLAY_NAME("creator_display_name", PlayableMetadata.KEY_CREATOR_NAME),
    CREATOR_URN("creator_urn", PlayableMetadata.KEY_CREATOR_URN),
    PLAYABLE_TITLE("playable_title", PlayableMetadata.KEY_PLAYABLE_TITLE),
    PLAYABLE_URN("playable_urn", PlayableMetadata.KEY_PLAYABLE_URN),
    PLAYABLE_TYPE("playable_type", PlayableMetadata.KEY_PLAYABLE_TYPE),
    CATEGORY("category", ScreenEvent.KEY_SCREEN),
    GENRE("genre", ScreenEvent.KEY_GENRE);

    private final String eventKey;
    private final String appBoyKey;

    AppboyAttributeName(String appBoyKey, String eventKey) {
        this.appBoyKey = appBoyKey;
        this.eventKey = eventKey;
    }

    String getEventKey() {
        return eventKey;
    }

    String getAppBoyKey() {
        return appBoyKey;
    }
}



