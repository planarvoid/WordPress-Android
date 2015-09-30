package com.soundcloud.android.analytics.appboy;


import com.soundcloud.android.events.PlayableMetadata;

import java.util.Locale;

enum AppboyAttributeName {
    CREATOR_DISPLAY_NAME(PlayableMetadata.KEY_CREATOR_NAME),
    CREATOR_URN(PlayableMetadata.KEY_CREATOR_URN),
    PLAYABLE_TITLE(PlayableMetadata.KEY_PLAYABLE_TITLE),
    PLAYABLE_URN(PlayableMetadata.KEY_PLAYABLE_URN),
    PLAYABLE_TYPE(PlayableMetadata.KEY_PLAYABLE_TYPE);

    private final String key;

    AppboyAttributeName(String attributeKey) {
        this.key = attributeKey;
    }

    String getKey() {
        return key;
    }

    String getName() {
        return this.name().toLowerCase(Locale.US);
    }
}



