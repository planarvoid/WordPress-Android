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
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.NewTrackingEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class AppboyEventHandler {

    private static final List<AppboyAttributeName> PLAYABLE_ATTRIBUTES =
            Arrays.asList(CREATOR_DISPLAY_NAME, CREATOR_URN, PLAYABLE_TITLE, PLAYABLE_URN, PLAYABLE_TYPE);

    private static final List<AppboyAttributeName> EXPLORE_CATEGORY_ATTRIBUTES = Collections.singletonList(CATEGORY);

    private static final List<AppboyAttributeName> EXPLORE_GENRE_AND_CATEGORY = Arrays.asList(GENRE, CATEGORY);

    private static final List<AppboyAttributeName> CREATOR_ATTRIBUTES = Arrays.asList(CREATOR_DISPLAY_NAME,
                                                                                      CREATOR_URN);

    private static final List<AppboyAttributeName> PLAYLIST_ATTRIBUTES = Arrays.asList(PLAYLIST_TITLE, PLAYLIST_URN);

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
            case UIEvent.KIND_REPOST:
                tagEvent(AppboyEvents.REPOST, buildPlayableProperties(event));
                break;
            case UIEvent.KIND_CREATE_PLAYLIST:
                tagEvent(AppboyEvents.CREATE_PLAYLIST, buildPlaylistProperties(event));
                break;
            case UIEvent.KIND_START_STATION:
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
        if (event.kind() == NewTrackingEvent.Kind.SEARCH_SUBMIT && isSearchEverythingClick(event)) {
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
        if (isCategoryScreen(event)) {
            tagEvent(AppboyEvents.EXPLORE, buildProperties(EXPLORE_CATEGORY_ATTRIBUTES, event));
        } else if (isGenreScreen(event)) {
            tagEvent(AppboyEvents.EXPLORE, buildProperties(EXPLORE_GENRE_AND_CATEGORY, event));
        }
    }

    private boolean isCategoryScreen(ScreenEvent event) {
        return event.getScreenTag().equals(Screen.EXPLORE_GENRES.get())
                || event.getScreenTag().equals(Screen.EXPLORE_TRENDING_AUDIO.get())
                || event.getScreenTag().equals(Screen.EXPLORE_TRENDING_MUSIC.get());
    }

    private boolean isGenreScreen(ScreenEvent event) {
        return event.getScreenTag().startsWith(AppboyEvents.EXPLORE);
    }

    private AppboyProperties buildPlayableProperties(TrackingEvent event) {
        return buildProperties(PLAYABLE_ATTRIBUTES, event);
    }

    private AppboyProperties buildPlaylistProperties(UIEvent event) {
        return buildProperties(PLAYLIST_ATTRIBUTES, event);
    }

    private AppboyProperties buildCreatorProperties(UIEvent event) {
        return buildProperties(CREATOR_ATTRIBUTES, event);
    }

    private AppboyProperties buildOfflineProperties(String context, boolean isEnabled) {
        return new AppboyProperties()
                .addProperty(CONTEXT_PROPERTY, context)
                .addProperty(ENABLED_PROPERTY, isEnabled);
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
