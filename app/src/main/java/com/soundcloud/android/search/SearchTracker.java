package com.soundcloud.android.search;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
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
    private final SearchTypes searchTypes;
    private final Map<Screen, ScreenData> screenDataMap;
    private final FeatureOperations featureOperations;

    @Inject
    public SearchTracker(EventBus eventBus, FeatureOperations featureOperations, SearchTypes searchTypes) {
        this.eventBus = eventBus;
        this.searchTypes = searchTypes;
        this.screenDataMap = new EnumMap<>(Screen.class);
        this.featureOperations = featureOperations;
        initializeScreenQueryUrnMap();
    }

    private void initializeScreenQueryUrnMap() {
        for (SearchType searchType : searchTypes.available()) {
            this.screenDataMap.put(searchType.getScreen(), ScreenData.EMPTY);
        }
    }

    public void trackMainScreenEvent() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
    }

    void trackSearchSubmission(SearchType searchType, Urn queryUrn) {
        if (queryUrn != Urn.NOT_SET) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.searchStart(searchType.getScreen(),
                    new SearchQuerySourceInfo(queryUrn)));
        }
    }

    void trackSearchItemClick(SearchType searchType, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        publishItemClickEvent(searchType.getScreen(), urn, searchQuerySourceInfo);
    }

    void trackSearchPremiumItemClick(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        publishItemClickEvent(getPremiumTrackingScreen(), urn, searchQuerySourceInfo);
    }

    void trackResultsScreenEvent(SearchType searchType) {
        trackResultsScreenEvent(searchType.getScreen());
    }

    void trackResultsScreenEvent(Screen trackingScreen) {
        if (screenDataMap.containsKey(trackingScreen)) {
            final ScreenData screenData = screenDataMap.get(trackingScreen);
            final Urn queryUrn = screenData.queryUrn;
            if (queryUrn != Urn.NOT_SET) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(trackingScreen.get(),
                        new SearchQuerySourceInfo(queryUrn)));
            }
            final boolean hasPremiumContent = screenData.hasPremiumContent;
            if (hasPremiumContent && featureOperations.upsellHighTier()) {
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forSearchResultsImpression(trackingScreen));
            }
        }
    }

    void trackPremiumResultsScreenEvent(Urn queryUrn) {
        if (queryUrn != null && queryUrn != Urn.NOT_SET) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(getPremiumTrackingScreen().get(),
                    new SearchQuerySourceInfo(queryUrn)));
        }
    }

    void trackResultsUpsellClick(SearchType searchType) {
        eventBus.publish(EventQueue.TRACKING,
                UpgradeFunnelEvent.forSearchResultsClick(searchType.getScreen()));
    }

    void trackPremiumResultsUpsellImpression() {
        eventBus.publish(EventQueue.TRACKING,
                UpgradeFunnelEvent.forSearchPremiumResultsImpression(getPremiumTrackingScreen()));
    }

    void trackPremiumResultsUpsellClick() {
        eventBus.publish(EventQueue.TRACKING,
                UpgradeFunnelEvent.forSearchPremiumResultsClick(getPremiumTrackingScreen()));
    }

    void setTrackingData(SearchType searchType, Urn queryUrn, boolean hasPremiumContent) {
        if (screenDataMap.containsKey(searchType.getScreen())) {
            screenDataMap.put(searchType.getScreen(), new ScreenData(queryUrn, hasPremiumContent));
        }
    }

    void reset() {
        this.initializeScreenQueryUrnMap();
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
        static final ScreenData EMPTY = new ScreenData(Urn.NOT_SET, false);

        final Urn queryUrn;
        final boolean hasPremiumContent;

        private ScreenData(Urn queryUrn, boolean hasPremiumContent) {
            this.queryUrn = queryUrn;
            this.hasPremiumContent = hasPremiumContent;
        }
    }
}
