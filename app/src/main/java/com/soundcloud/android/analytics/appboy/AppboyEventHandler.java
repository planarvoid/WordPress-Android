package com.soundcloud.android.analytics.appboy;

import static com.soundcloud.android.analytics.appboy.AppboyAnalyticsProvider.TAG;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_DISPLAY_NAME;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_URN;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TITLE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TYPE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_URN;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;

import android.util.Log;

import java.util.Arrays;
import java.util.List;

class AppboyEventHandler {
    private static final List<AppboyAttributeName> PLAYABLE_ATTRIBUTES =
            Arrays.asList(CREATOR_DISPLAY_NAME, CREATOR_URN, PLAYABLE_TITLE, PLAYABLE_URN, PLAYABLE_TYPE);

    private final AppboyWrapper appboy;

    AppboyEventHandler(AppboyWrapper appboy) {
        this.appboy = appboy;
    }

    public void handleEvent(UIEvent event) {
        Log.d(TAG, "Handling UIEvent: " + event);
        switch (event.getKind()) {
            case UIEvent.KIND_LIKE:
                tagEvent(AppboyEvents.LIKE, buildPlayableProperties(event));
                break;
            case UIEvent.KIND_COMMENT:
                tagEvent(AppboyEvents.COMMENT, buildPlayableProperties(event));
                break;
            case UIEvent.KIND_SHARE:
                tagEvent(AppboyEvents.SHARE, buildPlayableProperties(event));
                break;
            default:
                break;
        }
    }

    public void handleEvent(PlaybackSessionEvent event) {
        Log.d(TAG, "Handling PlaybackSessionEvent: " + event);
        if (event.isPlayEvent() && event.isUserTriggered()) {
            tagEvent(AppboyEvents.PLAY, buildPlayableProperties(event));
            appboy.requestImmediateDataFlush();
        }
    }

    private AppboyProperties buildPlayableProperties(TrackingEvent event) {
        return buildProperties(PLAYABLE_ATTRIBUTES, event);
    }

    private AppboyProperties buildProperties(List<AppboyAttributeName> fields, TrackingEvent event) {
        AppboyProperties properties = new AppboyProperties();

        for (AppboyAttributeName attributeName : fields) {
            properties.addProperty(attributeName.getName(), event.get(attributeName.getKey()));
        }

        return properties;
    }

    private void tagEvent(String eventName, AppboyProperties properties) {
        appboy.logCustomEvent(eventName, properties);
    }
}
