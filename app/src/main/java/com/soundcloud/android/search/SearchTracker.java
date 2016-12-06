package com.soundcloud.android.search;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;


/**
 * Design discussions:
 * https://github.com/soundcloud/android/pull/4845
 * https://github.com/soundcloud/android/pull/4900
 */
@Singleton
public class SearchTracker {

    private final EventTracker eventTracker;
    private final FeatureOperations featureOperations;
    private final TrackingStateProvider trackingStateProvider;
    private Map<SearchType, ScreenData> screenDataMap;

    @Inject
    public SearchTracker(EventTracker eventTracker,
                         FeatureOperations featureOperations,
                         TrackingStateProvider trackingStateProvider) {
        this.eventTracker = eventTracker;
        this.featureOperations = featureOperations;
        this.trackingStateProvider = trackingStateProvider;
        init();
    }

    public void init() {
        this.screenDataMap = new EnumMap<>(SearchType.class);
        initializeScreenQueryUrnMap();
    }

    void trackSearchFormulationEnd(Screen screen, String query, Optional<Urn> queryUrn, Optional<Integer> queryPosition) {
        eventTracker.trackSearch(SearchEvent.searchFormulationEnd(screen, query, queryUrn, queryPosition));
    }

    private void initializeScreenQueryUrnMap() {
        for (SearchType searchType : SearchType.values()) {
            this.screenDataMap.put(searchType, ScreenData.EMPTY);
        }
    }

    public void trackMainScreenEvent() {
        eventTracker.trackScreen(ScreenEvent.create(Screen.SEARCH_MAIN), trackingStateProvider.getLastEvent());
    }

    void trackSearchSubmission(SearchType searchType, Urn queryUrn, String searchQuery) {
        if (queryUrn != Urn.NOT_SET) {
            eventTracker.trackSearch(SearchEvent.searchStart(searchType.getScreen(),
                                                             new SearchQuerySourceInfo(queryUrn,
                                                                                       searchQuery)));
        }
    }

    void trackSearchItemClick(SearchType searchType, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        publishItemClickEvent(searchType.getScreen(), urn, searchQuerySourceInfo);
    }

    void trackSearchPremiumItemClick(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        publishItemClickEvent(getPremiumTrackingScreen(), urn, searchQuerySourceInfo);
    }

    void trackResultsScreenEvent(SearchType searchType, String searchQuery) {
        //We can only track the page event when the page has been already loaded
        if (screenDataMap.get(searchType).isTrackingDataLoaded()) {
            final Screen trackingScreen = searchType.getScreen();
            final ScreenData screenData = screenDataMap.get(searchType);
            final Urn queryUrn = screenData.queryUrn;
            eventTracker.trackScreen(ScreenEvent.create(trackingScreen.get(),
                                                        new SearchQuerySourceInfo(queryUrn, searchQuery)),
                                     trackingStateProvider.getLastEvent());
            final boolean hasPremiumContent = screenData.hasPremiumContent;
            if (hasPremiumContent && featureOperations.upsellHighTier()) {
                eventTracker.trackUpgradeFunnel(UpgradeFunnelEvent.forSearchResultsImpression(trackingScreen));
            }
        } else {
            //If the page is not loaded, we save this state and fire the event after search is performed
            screenDataMap.put(searchType, ScreenData.EVENT_POSTPONED);
        }
    }

    void trackPremiumResultsScreenEvent(Urn queryUrn, String searchQuery) {
        if (queryUrn != null && queryUrn != Urn.NOT_SET) {
            eventTracker.trackScreen(ScreenEvent.create(getPremiumTrackingScreen().get(),
                                                        new SearchQuerySourceInfo(queryUrn, searchQuery)),
                                     trackingStateProvider.getLastEvent());
        }
    }

    void trackResultsUpsellClick(SearchType searchType) {
        eventTracker.trackUpgradeFunnel(UpgradeFunnelEvent.forSearchResultsClick(searchType.getScreen()));
    }

    void trackPremiumResultsUpsellImpression() {
        eventTracker.trackUpgradeFunnel(UpgradeFunnelEvent.forSearchPremiumResultsImpression(getPremiumTrackingScreen()));
    }

    void trackPremiumResultsUpsellClick() {
        eventTracker.trackUpgradeFunnel(UpgradeFunnelEvent.forSearchPremiumResultsClick(getPremiumTrackingScreen()));
    }

    boolean shouldSendResultsScreenEvent(SearchType searchType) {
        return screenDataMap.get(searchType).hasPostponedEvent();
    }

    void setTrackingData(SearchType searchType, Urn queryUrn, boolean hasPremiumContent) {
        screenDataMap.put(searchType, new ScreenData(queryUrn, hasPremiumContent, false));
    }

    void reset() {
        this.initializeScreenQueryUrnMap();
    }

    Screen getPremiumTrackingScreen() {
        return Screen.SEARCH_PREMIUM_CONTENT;
    }

    @VisibleForTesting
    Map<SearchType, ScreenData> getScreenDataMap() {
        return Collections.unmodifiableMap(screenDataMap);
    }

    private void publishItemClickEvent(Screen screen, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        final SearchResultItem searchResultItem = SearchResultItem.fromUrn(urn);
        if (searchResultItem.isTrack()) {
            eventTracker.trackSearch(SearchEvent.tapTrackOnScreen(screen, searchQuerySourceInfo));
        } else if (searchResultItem.isPlaylist()) {
            eventTracker.trackSearch(SearchEvent.tapPlaylistOnScreen(screen, searchQuerySourceInfo));
        } else if (searchResultItem.isUser()) {
            eventTracker.trackSearch(SearchEvent.tapUserOnScreen(screen, searchQuerySourceInfo));
        }
    }

    @VisibleForTesting
    static final class ScreenData {
        static final ScreenData EMPTY = new ScreenData(Urn.NOT_SET, false, false);
        static final ScreenData EVENT_POSTPONED = new ScreenData(Urn.NOT_SET, false, true);

        final Urn queryUrn;
        final boolean hasPremiumContent;
        final boolean postponedEvent;

        private ScreenData(Urn queryUrn, boolean hasPremiumContent, boolean postponedEvent) {
            this.queryUrn = queryUrn;
            this.hasPremiumContent = hasPremiumContent;
            this.postponedEvent = postponedEvent;
        }

        boolean isTrackingDataLoaded() {
            return queryUrn != Urn.NOT_SET;
        }

        boolean hasPostponedEvent() {
            return postponedEvent;
        }
    }
}
