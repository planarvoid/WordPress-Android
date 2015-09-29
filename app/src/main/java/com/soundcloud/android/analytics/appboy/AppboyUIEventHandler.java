package com.soundcloud.android.analytics.appboy;

import static com.soundcloud.android.analytics.appboy.AppboyAnalyticsProvider.TAG;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_DISPLAY_NAME;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_URN;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TITLE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TYPE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_URN;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.events.UIEvent;

import android.util.Log;

import java.util.Arrays;
import java.util.List;

class AppboyUIEventHandler {
    private static final List<AppboyAttributeName> PLAYABLE_ATTRIBUTES = Arrays.asList(
            CREATOR_DISPLAY_NAME, CREATOR_URN, PLAYABLE_TITLE, PLAYABLE_URN, PLAYABLE_TYPE);

    private final AppboyWrapper appboy;

    AppboyUIEventHandler(AppboyWrapper appboy) {
        this.appboy = appboy;
    }

    public void handleEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_LIKE:
                tagEvent(AppboyEvents.LIKE, PLAYABLE_ATTRIBUTES, event);
                break;
            default:
                break;
        }
    }

    private void tagEvent(String eventName, List<AppboyAttributeName> fields, UIEvent event) {
        Log.d(TAG, "handle UI Event: " + event);

        AppboyProperties properties = new AppboyProperties();

        for (AppboyAttributeName attributeName : fields) {
            properties.addProperty(attributeName.getName(), event.get(attributeName.getKey()));
        }

        appboy.logCustomEvent(eventName, properties);
    }
}
