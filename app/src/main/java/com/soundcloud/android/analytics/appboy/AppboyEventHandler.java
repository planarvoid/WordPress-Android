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
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class AppboyEventHandler {

    private static final List<AppboyAttributeName> PLAYABLE_ATTRIBUTES =
            Arrays.asList(CREATOR_DISPLAY_NAME, CREATOR_URN, PLAYABLE_TITLE, PLAYABLE_URN, PLAYABLE_TYPE);

    private static final List<AppboyAttributeName> CREATOR_ATTRIBUTES = Arrays.asList(CREATOR_DISPLAY_NAME,
                                                                                      CREATOR_URN);

    private static final String ENABLED_PROPERTY = "enabled";
    private static final String CONTEXT_PROPERTY = "context";
    private static final String LIKES_CONTEXT = "likes";
    private static final String PLAYLIST_CONTEXT = "playlist";
    private static final String ALL_CONTEXT = "all";
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
        switch (event.getKind()) {
            case OfflineInteractionEvent.KIND_OFFLINE_LIKES_ADD:
                tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(LIKES_CONTEXT, true));
                break;
            case OfflineInteractionEvent.KIND_OFFLINE_LIKES_REMOVE:
                tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(LIKES_CONTEXT, false));
                break;
            case OfflineInteractionEvent.KIND_OFFLINE_PLAYLIST_ADD:
                tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(PLAYLIST_CONTEXT, true));
                break;
            case OfflineInteractionEvent.KIND_OFFLINE_PLAYLIST_REMOVE:
                tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(PLAYLIST_CONTEXT, false));
                break;
            case OfflineInteractionEvent.KIND_COLLECTION_SYNC_ENABLE:
                tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(ALL_CONTEXT, true));
                break;
            case OfflineInteractionEvent.KIND_COLLECTION_SYNC_DISABLE:
                tagEvent(AppboyEvents.OFFLINE_CONTENT, buildOfflineProperties(ALL_CONTEXT, false));
                break;
            default:
                break;
        }
    }

    void handleEvent(AttributionEvent event) {
        appboy.setAttribution(
                event.get(AttributionEvent.NETWORK),
                event.get(AttributionEvent.CAMPAIGN),
                event.get(AttributionEvent.ADGROUP),
                event.get(AttributionEvent.CREATIVE));
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

        if (!sessionPlayed && event.isPlayOrPlayStartEvent() && event.isMarketablePlay()) {
            appboyPlaySessionState.setSessionPlayed();
            tagEvent(AppboyEvents.PLAY, buildPlaybackProperties(event));
            appboy.requestInAppMessageRefresh();
            appboy.requestImmediateDataFlush();
        }
    }

    private AppboyProperties buildPlaybackProperties(PlaybackSessionEvent event) {
        final AppboyProperties properties = buildPlayableProperties(event);
        final String monetizationModel = event.getMonetizationModel();
        if (Strings.isNotBlank(monetizationModel)) {
            properties.addProperty(MONETIZATION_MODEL_PROPERTY, monetizationModel);
        }
        return properties;
    }

    void handleEvent(ScreenEvent event) {
        tagEvent(AppboyEvents.EXPLORE, buildProperties(event));
    }

    private AppboyProperties buildPlayableProperties(TrackingEvent event) {
        return buildProperties(PLAYABLE_ATTRIBUTES, event);
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
        return buildProperties(CREATOR_ATTRIBUTES, event);
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

    private AppboyProperties buildProperties(List<AppboyAttributeName> fields, TrackingEvent event) {
        AppboyProperties properties = new AppboyProperties();

        for (AppboyAttributeName attributeName : fields) {
            properties.addProperty(attributeName.getAppBoyKey(), event.get(attributeName.getEventKey()));
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
