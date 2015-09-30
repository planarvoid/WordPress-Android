package com.soundcloud.android.analytics.appboy;

import static com.soundcloud.android.analytics.Screen.SEARCH_EVERYTHING;
import static com.soundcloud.android.analytics.appboy.AppboyAnalyticsProvider.TAG;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_DISPLAY_NAME;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_URN;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TITLE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TYPE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_URN;
import static com.soundcloud.android.events.SearchEvent.CLICK_NAME_SEARCH;
import static com.soundcloud.android.events.SearchEvent.KEY_CLICK_NAME;
import static com.soundcloud.android.events.SearchEvent.KEY_PAGE_NAME;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.utils.Log;

import java.util.Arrays;
import java.util.List;

class AppboyEventHandler {
    private static final List<AppboyAttributeName> PLAYABLE_ATTRIBUTES =
            Arrays.asList(CREATOR_DISPLAY_NAME, CREATOR_URN, PLAYABLE_TITLE, PLAYABLE_URN, PLAYABLE_TYPE);

    private static final List<AppboyAttributeName> CREATOR_ATTRIBUTES =
            Arrays.asList(CREATOR_DISPLAY_NAME, CREATOR_URN);

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
            case UIEvent.KIND_FOLLOW:
                tagEvent(AppboyEvents.FOLLOW, buildCreatorProperties(event));
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

    private AppboyProperties buildCreatorProperties(UIEvent event) {
        return buildProperties(CREATOR_ATTRIBUTES, event);
    }

    public void handleEvent(SearchEvent event) {
        if (event.getKind().equals(SearchEvent.KIND_SUBMIT) && isSearchEverythingClick(event)) {
            tagEvent(AppboyEvents.SEARCH);
        }
    }

    private boolean isSearchEverythingClick(SearchEvent event) {
        return CLICK_NAME_SEARCH.equals(event.get(KEY_CLICK_NAME))
                && SEARCH_EVERYTHING.get().equals(event.get(KEY_PAGE_NAME));
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

    private void tagEvent(String eventName) {
        appboy.logCustomEvent(eventName);
    }

}
