package com.soundcloud.android.analytics.appboy;


import com.soundcloud.android.events.LocalyticTrackingKeys;
import com.soundcloud.android.events.UIEvent;

import java.util.Locale;

enum AppboyAttributeName {
    CREATOR_DISPLAY_NAME(UIEvent.KEY_CREATOR_NAME),
    CREATOR_URN(UIEvent.KEY_CREATOR_URN),
    PLAYABLE_TITLE(UIEvent.KEY_PLAYABLE_TITLE),
    PLAYABLE_URN(UIEvent.KEY_PLAYABLE_URN),
    PLAYABLE_TYPE(LocalyticTrackingKeys.KEY_RESOURCES_TYPE);

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



