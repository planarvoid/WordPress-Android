package com.soundcloud.android.analytics.appboy;

import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CATEGORY;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_DISPLAY_NAME;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.CREATOR_URN;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.GENRE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TITLE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_TYPE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYABLE_URN;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYLIST_TITLE;
import static com.soundcloud.android.analytics.appboy.AppboyAttributeName.PLAYLIST_URN;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;

class AppboyEventHandler {

    private static final String ENABLED_PROPERTY = "enabled";
    private static final String CONTEXT_PROPERTY = "context";
    private static final String MONETIZATION_MODEL_PROPERTY = "monetization_model";

    private final AppboyWrapper appboy;
    private final AppboyPlaySessionState appboyPlaySessionState;

    @Inject
    AppboyEventHandler(AppboyWrapper appboy, AppboyPlaySessionState appboyPlaySessionState) {
        this.appboy = appboy;
        this.appboyPlaySessionState = appboyPlaySessionState;
    }

    void handleEvent(UIEvent event) {
        switch (event.kind()) {
            case LIKE:
                tagEvent(AppboyEvents.LIKE, buildPlayableProperties(event));
                break;
            case FOLLOW:
                tagEvent(AppboyEvents.FOLLOW, buildCreatorProperties(event));
                break;
            case COMMENT:
                tagEvent(AppboyEvents.COMMENT, buildPlayableProperties(event));
                break;
            case SHARE:
                tagEvent(AppboyEvents.SHARE, buildPlayableProperties(event));
                break;
            case REPOST:
                tagEvent(AppboyEvents.REPOST, buildPlayableProperties(event));
                break;
            case CREATE_PLAYLIST:
                tagEvent(AppboyEvents.CREATE_PLAYLIST, buildPlaylistProperties(event));
                break;
            case START_STATION:
                tagEvent(AppboyEvents.START_STATION);
                break;
            default:
                break;
        }
    }

    void handleEvent(OfflineInteractionEvent event) {
        if (event.context().isPresent() && event.isEnabled().isPresent()) {
            tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(event.context().get().key(), event.isEnabled().get()));
        }
    }

    void handleEvent(AttributionEvent event) {
        appboy.setAttribution(
                event.network().or(Strings.EMPTY),
                event.campaign().or(Strings.EMPTY),
                event.adGroup().or(Strings.EMPTY),
                event.creative().or(Strings.EMPTY));
    }

    void handleEvent(SearchEvent event) {
        if (event.kind().isPresent() && event.kind().get() == SearchEvent.Kind.SUBMIT && isSearchEverythingClick(event)) {
            tagEvent(AppboyEvents.SEARCH);
        }
    }

    private boolean isSearchEverythingClick(SearchEvent event) {
        return event.clickName().isPresent()
                && event.clickName().get() == SearchEvent.ClickName.SEARCH
                && event.pageName().isPresent()
                && event.pageName().get().equals(Screen.SEARCH_EVERYTHING.get());
    }

    void handleEvent(PlaybackSessionEvent event) {
        boolean sessionPlayed = appboyPlaySessionState.isSessionPlayed();

        if (!sessionPlayed && event.isPlayOrPlayStartEvent() && event.marketablePlay()) {
            appboyPlaySessionState.setSessionPlayed();
            tagEvent(AppboyEvents.PLAY, buildPlaybackProperties(event));
            appboy.requestInAppMessageRefresh();
            appboy.requestImmediateDataFlush();
        }
    }

    private AppboyProperties buildPlaybackProperties(PlaybackSessionEvent event) {
        final AppboyProperties properties = buildPlayableProperties(event);
        final String monetizationModel = event.monetizationModel();
        if (Strings.isNotBlank(monetizationModel)) {
            properties.addProperty(MONETIZATION_MODEL_PROPERTY, monetizationModel);
        }
        return properties;
    }

    void handleEvent(ScreenEvent event) {
        tagEvent(AppboyEvents.EXPLORE, buildProperties(event));
    }

    private AppboyProperties buildPlayableProperties(PlaybackSessionEvent event) {
        AppboyProperties properties = new AppboyProperties();
        properties.addProperty(CREATOR_DISPLAY_NAME.getAppBoyKey(), event.creatorName());
        properties.addProperty(CREATOR_URN.getAppBoyKey(), event.creatorUrn().toString());
        properties.addProperty(PLAYABLE_TITLE.getAppBoyKey(), event.playableTitle());
        properties.addProperty(PLAYABLE_URN.getAppBoyKey(), event.playableUrn().toString());
        properties.addProperty(PLAYABLE_TYPE.getAppBoyKey(), event.playableType());
        return properties;
    }

    private AppboyProperties buildPlayableProperties(UIEvent event) {
        AppboyProperties properties = new AppboyProperties();
        if (event.creatorName().isPresent()) {
            properties.addProperty(CREATOR_DISPLAY_NAME.getAppBoyKey(), event.creatorName().get());
        }
        if (event.creatorUrn().isPresent()) {
            properties.addProperty(CREATOR_URN.getAppBoyKey(), event.creatorUrn().get().toString());
        }
        if (event.playableTitle().isPresent()) {
            properties.addProperty(PLAYABLE_TITLE.getAppBoyKey(), event.playableTitle().get());
        }
        if (event.playableUrn().isPresent()) {
            properties.addProperty(PLAYABLE_URN.getAppBoyKey(), event.playableUrn().get().toString());
        }
        if (event.playableType().isPresent()) {
            properties.addProperty(PLAYABLE_TYPE.getAppBoyKey(), event.playableType().get());
        }
        return properties;
    }

    private AppboyProperties buildPlaylistProperties(UIEvent event) {
        AppboyProperties properties = new AppboyProperties();

        if (event.playableTitle().isPresent()) {
            properties.addProperty(PLAYLIST_TITLE.getAppBoyKey(), event.playableTitle().get());
        }
        if (event.playableUrn().isPresent()) {
            properties.addProperty(PLAYLIST_URN.getAppBoyKey(), event.playableUrn().get().toString());
        }
        return properties;
    }

    private AppboyProperties buildCreatorProperties(UIEvent event) {
        AppboyProperties properties = new AppboyProperties();

        if (event.creatorName().isPresent()) {
            properties.addProperty(CREATOR_DISPLAY_NAME.getAppBoyKey(), event.creatorName().get());
        }

        if (event.creatorUrn().isPresent()) {
            properties.addProperty(CREATOR_URN.getAppBoyKey(), event.creatorUrn().get().toString());
        }

        return properties;
    }

    private AppboyProperties buildOfflineProperties(String context, boolean isEnabled) {
        return new AppboyProperties()
                .addProperty(CONTEXT_PROPERTY, context)
                .addProperty(ENABLED_PROPERTY, isEnabled);
    }

    private AppboyProperties buildProperties(ScreenEvent screenEvent) {
        AppboyProperties properties = new AppboyProperties();
        if (screenEvent.genre().isPresent()) {
            properties.addProperty(GENRE.getAppBoyKey(), screenEvent.genre().get());
        }
        properties.addProperty(CATEGORY.getAppBoyKey(), screenEvent.screen());
        return properties;
    }

    private void tagEvent(String eventName, AppboyProperties properties) {
        appboy.logCustomEvent(eventName, properties);
    }

    private void tagEvent(String eventName) {
        appboy.logCustomEvent(eventName);
    }

}
