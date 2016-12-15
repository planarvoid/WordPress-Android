package com.soundcloud.android.analytics.appboy;


import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.java.strings.Strings;

public enum AppboyAttributeName {
    CREATOR_DISPLAY_NAME("creator_display_name", EntityMetadata.KEY_CREATOR_NAME),
    CREATOR_URN("creator_urn", EntityMetadata.KEY_CREATOR_URN),
    PLAYABLE_TITLE("playable_title", EntityMetadata.KEY_PLAYABLE_TITLE),
    PLAYABLE_URN("playable_urn", EntityMetadata.KEY_PLAYABLE_URN),
    PLAYABLE_TYPE("playable_type", EntityMetadata.KEY_PLAYABLE_TYPE),
    PLAYLIST_TITLE("playlist_title", EntityMetadata.KEY_PLAYABLE_TITLE),
    PLAYLIST_URN("playlist_urn", EntityMetadata.KEY_PLAYABLE_URN),
    CATEGORY("category", Strings.EMPTY),
    GENRE("genre", Strings.EMPTY);

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



