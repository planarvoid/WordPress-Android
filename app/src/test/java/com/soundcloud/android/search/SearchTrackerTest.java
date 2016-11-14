package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchTracker.ScreenData;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SearchTrackerTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(1L);
    private static final Urn USER_URN = Urn.forUser(1L);
    private static final Urn QUERY_URN = new Urn("soundcloud:search:123");
    private static final int SEARCH_RESULT_TYPES = 5;
    private static final String SEARCH_QUERY = "query";

    private SearchTracker tracker;

    @Mock private EventTracker eventTracker;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Captor private ArgumentCaptor<SearchEvent> searchEventCaptor;
    @Captor private ArgumentCaptor<ScreenEvent> screenEventCaptor;
    @Captor private ArgumentCaptor<UpgradeFunnelEvent> upgradeFunnelEventCaptor;
    @Captor private ArgumentCaptor<Optional<ReferringEvent>> referringEventCaptor;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        when(trackingStateProvider.getLastEvent()).thenReturn(Optional.<ReferringEvent>absent());
        tracker = new SearchTracker(eventTracker, featureOperations, trackingStateProvider);
    }

    @Test
    public void mustTrackSearchMainScreenEvent() {
        tracker.trackMainScreenEvent();

        verify(eventTracker).trackScreen(screenEventCaptor.capture(), referringEventCaptor.capture());

        assertThat(screenEventCaptor.getValue().getScreenTag()).isEqualTo(Screen.SEARCH_MAIN.get());
    }

    @Test
    public void mustTrackSearchSubmissionWithQueryUrn() {
        tracker.trackSearchSubmission(SearchType.ALL, QUERY_URN, SEARCH_QUERY);

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        assertThat(searchEventCaptor.getValue().getKind()).isEqualTo(SearchEvent.KIND_SUBMIT);
        assertThat(searchEventCaptor.getValue().get(SearchEvent.KEY_QUERY_URN)).isEqualTo(QUERY_URN.toString());
    }

    @Test
    public void mustNotTrackSearchSubmissionWithoutQueryUrn() {
        tracker.trackSearchSubmission(SearchType.ALL, Urn.NOT_SET, SEARCH_QUERY);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void mustSetQueryUrnForScreen() {
        tracker.setTrackingData(SearchType.ALL, QUERY_URN, false);

        final Map<SearchType, ScreenData> screenDataMap = tracker.getScreenDataMap();
        final Urn searchEverythingUrn = screenDataMap.get(SearchType.ALL).queryUrn;
        final Urn searchTracksUrn = screenDataMap.get(SearchType.TRACKS).queryUrn;
        final Urn searchPlaylistsUrn = screenDataMap.get(SearchType.PLAYLISTS).queryUrn;
        final Urn searchUsersUrn = screenDataMap.get(SearchType.USERS).queryUrn;

        assertThat(searchEverythingUrn).isEqualTo(QUERY_URN);
        assertThat(searchTracksUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void mustSetHasPremiumContentValueForScreen() {
        tracker.setTrackingData(SearchType.ALL, QUERY_URN, true);

        final Map<SearchType, ScreenData> screenDataMap = tracker.getScreenDataMap();
        final boolean searchEverythingHasPremiumContent = screenDataMap.get(SearchType.ALL).hasPremiumContent;
        final boolean searchTracksUrnHasPremiumContent = screenDataMap.get(SearchType.TRACKS).hasPremiumContent;
        final boolean searchPlaylistsUrnHasPremiumContent = screenDataMap.get(SearchType.PLAYLISTS).hasPremiumContent;
        final boolean searchUsersUrnHasPremiumContent = screenDataMap.get(SearchType.USERS).hasPremiumContent;

        assertThat(searchEverythingHasPremiumContent).isTrue();
        assertThat(searchTracksUrnHasPremiumContent).isFalse();
        assertThat(searchPlaylistsUrnHasPremiumContent).isFalse();
        assertThat(searchUsersUrnHasPremiumContent).isFalse();
    }

    @Test
    public void mustTrackResultsScreenEventWithValidSearchScreen() {
        tracker.setTrackingData(SearchType.ALL, QUERY_URN, true);
        tracker.trackResultsScreenEvent(SearchType.ALL, SEARCH_QUERY);

        verify(eventTracker).trackScreen(screenEventCaptor.capture(), referringEventCaptor.capture());

        assertThat(screenEventCaptor.getValue().getScreenTag()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
        assertThat(screenEventCaptor.getValue().get(SearchEvent.KEY_QUERY_URN)).isEqualTo(QUERY_URN.toString());
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidScreen() {
        tracker.setTrackingData(SearchType.TRACKS, QUERY_URN, true);
        tracker.trackResultsScreenEvent(SearchType.USERS, SEARCH_QUERY);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidQueryUrnNotSet() {
        tracker.trackResultsScreenEvent(SearchType.ALL, SEARCH_QUERY);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void mustResetTrackingState() {
        assertTrackerInitialState();

        tracker.setTrackingData(SearchType.ALL, QUERY_URN, false);

        final Map<SearchType, ScreenData> screenDataMap = tracker.getScreenDataMap();
        assertThat(screenDataMap.get(SearchType.ALL).queryUrn).isEqualTo(QUERY_URN);

        tracker.reset();

        assertTrackerInitialState();
    }

    @Test
    public void trackItemClickOnSearchResultsEverythingPublishesEvent() {
        tracker.trackSearchItemClick(SearchType.ALL, TRACK_URN, new SearchQuerySourceInfo(QUERY_URN, 0, TRACK_URN,
                                                                                          SEARCH_QUERY));

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        final SearchEvent event = searchEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("track");
        assertThat(event.getAttributes().get("context")).isEqualTo("everything");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:everything");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void trackItemClickOnSearchResultsTracksPublishesEvent() {
        tracker.trackSearchItemClick(SearchType.TRACKS, TRACK_URN, new SearchQuerySourceInfo(QUERY_URN, 0, TRACK_URN,
                                                                                             SEARCH_QUERY));

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        final SearchEvent event = searchEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("track");
        assertThat(event.getAttributes().get("context")).isEqualTo("tracks");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:tracks");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void trackItemClickOnSearchResultsPlaylistsPublishesEvent() {
        tracker.trackSearchItemClick(SearchType.PLAYLISTS,
                                     PLAYLIST_URN,
                                     new SearchQuerySourceInfo(QUERY_URN, 1, PLAYLIST_URN, SEARCH_QUERY));

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        final SearchEvent event = searchEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("playlist");
        assertThat(event.getAttributes().get("context")).isEqualTo("playlists");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:playlists");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:playlists:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(1);
    }

    @Test
    public void trackItemClickOnSearchResultsUsersPublishesEvent() {
        tracker.trackSearchItemClick(SearchType.USERS, USER_URN, new SearchQuerySourceInfo(QUERY_URN, 0, USER_URN,
                                                                                           SEARCH_QUERY));

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        final SearchEvent event = searchEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("user");
        assertThat(event.getAttributes().get("context")).isEqualTo("people");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:people");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:users:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void trackItemClickOnSearchPremiumResultsTracksPublishesEvent() {
        tracker.trackSearchPremiumItemClick(TRACK_URN,
                                            new SearchQuerySourceInfo(QUERY_URN, 0, TRACK_URN, SEARCH_QUERY));

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        final SearchEvent event = searchEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("track");
        assertThat(event.getAttributes().get("context")).isEqualTo("premium");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:high_tier");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void mustTrackPremiumResultsScreenEventWithValidQueryUrn() {
        tracker.trackPremiumResultsScreenEvent(QUERY_URN, SEARCH_QUERY);

        verify(eventTracker).trackScreen(screenEventCaptor.capture(), referringEventCaptor.capture());

        final ScreenEvent event = screenEventCaptor.getValue();

        assertThat(event.getScreenTag()).isEqualTo(Screen.SEARCH_PREMIUM_CONTENT.get());
        assertThat(event.get(SearchEvent.KEY_QUERY_URN)).isEqualTo(QUERY_URN.toString());
    }

    @Test
    public void mustNotTrackPremiumResultsScreenEventWithQueryUrnNotSet() {
        tracker.trackPremiumResultsScreenEvent(Urn.NOT_SET, SEARCH_QUERY);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void mustNotTrackUpsellImpressionIfNotUpsellUser() {
        when(featureOperations.upsellHighTier()).thenReturn(false);

        tracker.setTrackingData(SearchType.ALL, Urn.NOT_SET, true);
        tracker.trackResultsScreenEvent(SearchType.ALL, SEARCH_QUERY);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void mustNotTrackUpsellImpressionIfThereIsNoPremiumContent() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        tracker.trackResultsScreenEvent(SearchType.ALL, SEARCH_QUERY);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void mustTrackUpsellImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        tracker.setTrackingData(SearchType.ALL, QUERY_URN, true);
        tracker.trackResultsScreenEvent(SearchType.ALL, SEARCH_QUERY);

        verify(eventTracker).trackScreen(screenEventCaptor.capture(), referringEventCaptor.capture());

        final ScreenEvent event = screenEventCaptor.getValue();
        assertThat(event.getScreenTag()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void mustTrackResultsUpsellClick() {
        tracker.trackResultsUpsellClick(SearchType.ALL);

        verify(eventTracker).trackUpgradeFunnel(upgradeFunnelEventCaptor.capture());

        assertThat(upgradeFunnelEventCaptor.getValue().getKind()).isEqualTo(UpgradeFunnelEvent.KIND_UPSELL_CLICK);
    }

    @Test
    public void trackPremiumResultsUpsellImpression() {
        tracker.trackPremiumResultsUpsellClick();

        verify(eventTracker).trackUpgradeFunnel(upgradeFunnelEventCaptor.capture());

        final TrackingEvent event = upgradeFunnelEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.KIND_UPSELL_CLICK);
    }

    @Test
    public void mustTrackPremiumResultsUpsellClick() {
        tracker.trackPremiumResultsUpsellImpression();

        verify(eventTracker).trackUpgradeFunnel(upgradeFunnelEventCaptor.capture());

        final TrackingEvent event = upgradeFunnelEventCaptor.getValue();
        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.KIND_UPSELL_IMPRESSION);
    }

    @Test
    public void mustSetHasPremiumContentValueForSearchType() {
        assertThat(tracker.getScreenDataMap().get(SearchType.ALL).hasPremiumContent).isFalse();

        tracker.setTrackingData(SearchType.ALL, QUERY_URN, true);

        assertThat(tracker.getScreenDataMap().get(SearchType.ALL).hasPremiumContent).isTrue();
    }

    private void assertTrackerInitialState() {
        final Map<SearchType, ScreenData> screenDataMap = tracker.getScreenDataMap();
        assertThat(screenDataMap.size()).isEqualTo(SEARCH_RESULT_TYPES);

        final Urn searchEverythingUrn = screenDataMap.get(SearchType.ALL).queryUrn;
        final Urn searchUsersUrn = screenDataMap.get(SearchType.USERS).queryUrn;
        final Urn searchTracksUrn = screenDataMap.get(SearchType.TRACKS).queryUrn;
        final Urn searchAlbumsUrn = screenDataMap.get(SearchType.ALBUMS).queryUrn;
        final Urn searchPlaylistsUrn = screenDataMap.get(SearchType.PLAYLISTS).queryUrn;
        assertThat(searchEverythingUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchTracksUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchAlbumsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);

        final boolean searchEverythingHasPremiumContent = screenDataMap.get(SearchType.ALL).hasPremiumContent;
        final boolean searchUsersUrnHasPremiumContent = screenDataMap.get(SearchType.USERS).hasPremiumContent;
        final boolean searchTracksUrnHasPremiumContent = screenDataMap.get(SearchType.TRACKS).hasPremiumContent;
        final boolean searchAlbumsUrnHasPremiumContent = screenDataMap.get(SearchType.ALBUMS).hasPremiumContent;
        final boolean searchPlaylistsUrnHasPremiumContent = screenDataMap.get(SearchType.PLAYLISTS).hasPremiumContent;
        assertThat(searchEverythingHasPremiumContent).isFalse();
        assertThat(searchUsersUrnHasPremiumContent).isFalse();
        assertThat(searchTracksUrnHasPremiumContent).isFalse();
        assertThat(searchAlbumsUrnHasPremiumContent).isFalse();
        assertThat(searchPlaylistsUrnHasPremiumContent).isFalse();
    }
}