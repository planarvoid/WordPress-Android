package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;


/**
 * Design discussion: https://github.com/soundcloud/SoundCloud-Android/pull/4845
 */
@Singleton
public class SearchTracker {

    private final EventBus eventBus;

    private final Map<Screen, Urn> screenQueryUrnMap;

    @Inject
    public SearchTracker(EventBus eventBus) {
        this.eventBus = eventBus;
        this.screenQueryUrnMap = new EnumMap<>(Screen.class);
        initializeScreenQueryUrnMap();
    }

    private void initializeScreenQueryUrnMap() {
        this.screenQueryUrnMap.put(Screen.SEARCH_EVERYTHING, Urn.NOT_SET);
        this.screenQueryUrnMap.put(Screen.SEARCH_TRACKS, Urn.NOT_SET);
        this.screenQueryUrnMap.put(Screen.SEARCH_PLAYLISTS, Urn.NOT_SET);
        this.screenQueryUrnMap.put(Screen.SEARCH_USERS, Urn.NOT_SET);
    }

    public void trackMainScreenEvent() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
    }

    void trackSearchSubmission(int searchType, Optional<Urn> optionalQueryUrn) {
        if (optionalQueryUrn.isPresent()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.searchStart(getTrackingScreen(searchType),
                    new SearchQuerySourceInfo(optionalQueryUrn.get())));
        }
    }

    void trackSearchItemClick(int searchType, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        final SearchResultItem searchResultItem = SearchResultItem.fromUrn(urn);
        if (searchResultItem.isTrack()) {
            eventBus.publish(EventQueue.TRACKING,
                    SearchEvent.tapTrackOnScreen(getTrackingScreen(searchType), searchQuerySourceInfo));
        } else if (searchResultItem.isPlaylist()) {
            eventBus.publish(EventQueue.TRACKING,
                    SearchEvent.tapPlaylistOnScreen(getTrackingScreen(searchType), searchQuerySourceInfo));
        } else if (searchResultItem.isUser()) {
            eventBus.publish(EventQueue.TRACKING,
                    SearchEvent.tapUserOnScreen(getTrackingScreen(searchType), searchQuerySourceInfo));
        }
    }

    void trackResultsScreenEvent(int searchType) {
        final Screen trackingScreen = getTrackingScreen(searchType);
        if (screenQueryUrnMap.containsKey(trackingScreen)) {
            final Urn queryUrn = screenQueryUrnMap.get(trackingScreen);
            if (queryUrn != Urn.NOT_SET) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(trackingScreen.get(),
                        new SearchQuerySourceInfo(queryUrn)));
            }
        }
    }

    void setQueryUrnForSearchType(int searchType, Optional<Urn> optionalQueryUrn) {
        final Screen trackingScreen = getTrackingScreen(searchType);
        if (screenQueryUrnMap.containsKey(trackingScreen) && optionalQueryUrn.isPresent()) {
            screenQueryUrnMap.put(trackingScreen, optionalQueryUrn.get());
        }
    }

    void reset() {
        this.initializeScreenQueryUrnMap();
    }

    Screen getTrackingScreen(int searchType) {
        switch (searchType) {
            case TYPE_ALL:
                return Screen.SEARCH_EVERYTHING;
            case TYPE_TRACKS:
                return Screen.SEARCH_TRACKS;
            case TYPE_PLAYLISTS:
                return Screen.SEARCH_PLAYLISTS;
            case TYPE_USERS:
                return Screen.SEARCH_USERS;
            default:
                throw new IllegalArgumentException("Search query type not valid.");
        }
    }

    @VisibleForTesting
    Map<Screen, Urn> getScreenQueryUrnMap() {
        return Collections.unmodifiableMap(screenQueryUrnMap);
    }
}
