package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;


/**
 * Design discussions:
 * https://github.com/soundcloud/SoundCloud-Android/pull/4845
 * https://github.com/soundcloud/SoundCloud-Android/pull/4900
 */
@Singleton
public class SearchTracker {

    private final EventBus eventBus;
    private final Map<Screen, ScreenData> screenDataMap;
    private final FeatureOperations featureOperations;

    @Inject
    public SearchTracker(EventBus eventBus, FeatureOperations featureOperations) {
        this.eventBus = eventBus;
        this.screenDataMap = new EnumMap<>(Screen.class);
        this.featureOperations = featureOperations;
        initializeScreenQueryUrnMap();
    }

    private void initializeScreenQueryUrnMap() {
        this.screenDataMap.put(Screen.SEARCH_EVERYTHING, new ScreenData());
        this.screenDataMap.put(Screen.SEARCH_TRACKS, new ScreenData());
        this.screenDataMap.put(Screen.SEARCH_PLAYLISTS, new ScreenData());
        this.screenDataMap.put(Screen.SEARCH_USERS, new ScreenData());
    }

    public void trackMainScreenEvent() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
    }

    void trackSearchSubmission(int searchType, Urn queryUrn) {
        if (queryUrn != Urn.NOT_SET) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.searchStart(getTrackingScreen(searchType),
                    new SearchQuerySourceInfo(queryUrn)));
        }
    }

    void trackSearchItemClick(int searchType, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        publishItemClickEvent(getTrackingScreen(searchType), urn, searchQuerySourceInfo);
    }

    void trackSearchPremiumItemClick(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        publishItemClickEvent(getPremiumTrackingScreen(), urn, searchQuerySourceInfo);
    }

    void trackResultsScreenEvent(int searchType) {
        final Screen trackingScreen = getTrackingScreen(searchType);
        if (screenDataMap.containsKey(trackingScreen)) {
            final Urn queryUrn = screenDataMap.get(trackingScreen).queryUrn;
            if (queryUrn != Urn.NOT_SET) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(trackingScreen.get(),
                        new SearchQuerySourceInfo(queryUrn)));
            }
            final boolean hasPremiumContent = screenDataMap.get(trackingScreen).hasPremiumContent;
            if (hasPremiumContent && featureOperations.upsellHighTier()) {
                eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forSearchResultsImpression(trackingScreen));
            }
        }
    }

    void trackPremiumResultsScreenEvent(Urn queryUrn) {
        if (queryUrn != null && queryUrn != Urn.NOT_SET) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(getPremiumTrackingScreen().get(),
                    new SearchQuerySourceInfo(queryUrn)));
        }
    }

    void trackResultsUpsellClick(int searchType) {
        eventBus.publish(EventQueue.TRACKING,
                UpgradeTrackingEvent.forSearchResultsClick(getTrackingScreen(searchType)));
    }

    void trackPremiumResultsUpsellImpression() {
        eventBus.publish(EventQueue.TRACKING,
                UpgradeTrackingEvent.forSearchPremiumResultsImpression(getPremiumTrackingScreen()));
    }

    void trackPremiumResultsUpsellClick() {
        eventBus.publish(EventQueue.TRACKING,
                UpgradeTrackingEvent.forSearchPremiumResultsClick(getPremiumTrackingScreen()));
    }

    void setTrackingData(int searchType, Urn queryUrn, boolean hasPremiumContent) {
        final Screen trackingScreen = getTrackingScreen(searchType);
        if (screenDataMap.containsKey(trackingScreen)) {
            final ScreenData screenData = screenDataMap.get(trackingScreen);
            screenData.queryUrn = queryUrn;
            screenData.hasPremiumContent = hasPremiumContent;
            screenDataMap.put(trackingScreen, screenData);
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

    Screen getPremiumTrackingScreen() {
        return Screen.SEARCH_PREMIUM_CONTENT;
    }

    @VisibleForTesting
    Map<Screen, ScreenData> getScreenDataMap() {
        return Collections.unmodifiableMap(screenDataMap);
    }

    private void publishItemClickEvent(Screen screen, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        final SearchResultItem searchResultItem = SearchResultItem.fromUrn(urn);
        if (searchResultItem.isTrack()) {
            eventBus.publish(EventQueue.TRACKING,
                    SearchEvent.tapTrackOnScreen(screen, searchQuerySourceInfo));
        } else if (searchResultItem.isPlaylist()) {
            eventBus.publish(EventQueue.TRACKING,
                    SearchEvent.tapPlaylistOnScreen(screen, searchQuerySourceInfo));
        } else if (searchResultItem.isUser()) {
            eventBus.publish(EventQueue.TRACKING,
                    SearchEvent.tapUserOnScreen(screen, searchQuerySourceInfo));
        }
    }

    @VisibleForTesting
    static final class ScreenData {
        Urn queryUrn;
        boolean hasPremiumContent;

        private ScreenData() {
            this.queryUrn = Urn.NOT_SET;
            this.hasPremiumContent = false;
        }
    }
}
