package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.SearchTracker.ScreenData;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SearchTrackerTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(1L);
    private static final Urn USER_URN = Urn.forUser(1L);
    private static final Urn QUERY_URN = new Urn("soundcloud:search:123");
    private static final int SEARCH_RESULT_TYPES = 4;

    private SearchTracker tracker;
    private TestEventBus eventBus = new TestEventBus();

    @Mock FeatureOperations featureOperations;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        tracker = new SearchTracker(eventBus, featureOperations);
    }

    @Test
    public void mustTrackSearchMainScreenEvent() {
        tracker.trackMainScreenEvent();

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_MAIN));
    }

    @Test
    public void mustTrackSearchSubmissionWithQueryUrn() {
        tracker.trackSearchSubmission(TYPE_ALL, QUERY_URN);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_EVERYTHING));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(QUERY_URN));
    }

    @Test
    public void mustNotTrackSearchSubmissionWithoutQueryUrn() {
        tracker.trackSearchSubmission(TYPE_ALL, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustSetQueryUrnForScreen() {
        tracker.setTrackingData(TYPE_ALL, QUERY_URN, false);

        final Map<Screen, ScreenData> screenDataMap = tracker.getScreenDataMap();
        final Urn searchEverythingUrn = screenDataMap.get(Screen.SEARCH_EVERYTHING).queryUrn;
        final Urn searchTracksUrn = screenDataMap.get(Screen.SEARCH_TRACKS).queryUrn;
        final Urn searchPlaylistsUrn = screenDataMap.get(Screen.SEARCH_PLAYLISTS).queryUrn;
        final Urn searchUsersUrn = screenDataMap.get(Screen.SEARCH_USERS).queryUrn;

        assertThat(searchEverythingUrn).isEqualTo(QUERY_URN);
        assertThat(searchTracksUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void mustSetHasPremiumContentValueForScreen() {
        tracker.setTrackingData(TYPE_ALL, QUERY_URN, true);

        final Map<Screen, ScreenData> screenDataMap = tracker.getScreenDataMap();
        final boolean searchEverythingHasPremiumContent = screenDataMap.get(Screen.SEARCH_EVERYTHING).hasPremiumContent;
        final boolean searchTracksUrnHasPremiumContent = screenDataMap.get(Screen.SEARCH_TRACKS).hasPremiumContent;
        final boolean searchPlaylistsUrnHasPremiumContent = screenDataMap.get(Screen.SEARCH_PLAYLISTS).hasPremiumContent;
        final boolean searchUsersUrnHasPremiumContent = screenDataMap.get(Screen.SEARCH_USERS).hasPremiumContent;

        assertThat(searchEverythingHasPremiumContent).isTrue();
        assertThat(searchTracksUrnHasPremiumContent).isFalse();
        assertThat(searchPlaylistsUrnHasPremiumContent).isFalse();
        assertThat(searchUsersUrnHasPremiumContent).isFalse();
    }

    @Test
    public void mustTrackResultsScreenEventWithValidSearchScreen() {
        tracker.setTrackingData(TYPE_ALL, QUERY_URN, true);
        tracker.trackResultsScreenEvent(TYPE_ALL);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_TRACKS));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(QUERY_URN));
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidScreen() {
        tracker.setTrackingData(TYPE_TRACKS, QUERY_URN, true);
        tracker.trackResultsScreenEvent(TYPE_USERS);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidQueryUrnNotSet() {
        tracker.trackResultsScreenEvent(TYPE_ALL);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustResetTrackingState() {
        assertTrackerInitialState();

        tracker.setTrackingData(TYPE_ALL, QUERY_URN, false);

        final Map<Screen, ScreenData> screenDataMap = tracker.getScreenDataMap();
        assertThat(screenDataMap.get(Screen.SEARCH_EVERYTHING).queryUrn).isEqualTo(QUERY_URN);

        tracker.reset();

        assertTrackerInitialState();
    }

    @Test
    public void trackItemClickOnSearchResultsEverythingPublishesEvent() {
        tracker.trackSearchItemClick(TYPE_ALL, TRACK_URN, new SearchQuerySourceInfo(QUERY_URN, 0, TRACK_URN));

        final SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
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
        tracker.trackSearchItemClick(TYPE_TRACKS, TRACK_URN, new SearchQuerySourceInfo(QUERY_URN, 0, TRACK_URN));

        final SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
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
        tracker.trackSearchItemClick(TYPE_PLAYLISTS, PLAYLIST_URN, new SearchQuerySourceInfo(QUERY_URN, 1, PLAYLIST_URN));

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
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
        tracker.trackSearchItemClick(TYPE_USERS, USER_URN, new SearchQuerySourceInfo(QUERY_URN, 0, USER_URN));

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
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
        tracker.trackSearchPremiumItemClick(TRACK_URN, new SearchQuerySourceInfo(QUERY_URN, 0, TRACK_URN));

        final SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
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
        tracker.trackPremiumResultsScreenEvent(QUERY_URN);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_PREMIUM_CONTENT));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(QUERY_URN));
    }

    @Test
    public void mustNotTrackPremiumResultsScreenEventWithQueryUrnNotSet() {
        tracker.trackPremiumResultsScreenEvent(Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustThrowExceptionWhenTrackingWithInvalidSearchType() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Search query type not valid.");

        tracker.trackResultsScreenEvent(Integer.MAX_VALUE);
    }

    @Test
    public void mustNotTrackUpsellImpressionIfNotUpsellUser() {
        when(featureOperations.upsellHighTier()).thenReturn(false);

        tracker.setTrackingData(TYPE_ALL, Urn.NOT_SET, true);
        tracker.trackResultsScreenEvent(TYPE_ALL);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustNotTrackUpsellImpressionIfThereIsNoPremiumContent() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        tracker.trackResultsScreenEvent(TYPE_ALL);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustTrackUpsellImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        tracker.setTrackingData(TYPE_ALL, Urn.NOT_SET, true);
        tracker.trackResultsScreenEvent(TYPE_ALL);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void mustTrackResultsUpsellClick() {
        tracker.trackResultsUpsellClick(TYPE_ALL);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void trackPremiumResultsUpsellImpression() {
        tracker.trackPremiumResultsUpsellClick();

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_PREMIUM_CONTENT));
    }

    @Test
    public void mustTrackPremiumResultsUpsellClick() {
        tracker.trackPremiumResultsUpsellImpression();

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_PREMIUM_CONTENT));
    }

    @Test
    public void mustSetHasPremiumContentValueForSearchType() {
        assertThat(tracker.getScreenDataMap().get(Screen.SEARCH_EVERYTHING).hasPremiumContent).isFalse();

        tracker.setTrackingData(TYPE_ALL, QUERY_URN, true);

        assertThat(tracker.getScreenDataMap().get(Screen.SEARCH_EVERYTHING).hasPremiumContent).isTrue();
    }

    private void assertTrackerInitialState() {
        final Map<Screen, ScreenData> screenDataMap = tracker.getScreenDataMap();
        assertThat(screenDataMap.size()).isEqualTo(SEARCH_RESULT_TYPES);

        final Urn searchEverythingUrn = screenDataMap.get(Screen.SEARCH_EVERYTHING).queryUrn;
        final Urn searchTracksUrn = screenDataMap.get(Screen.SEARCH_TRACKS).queryUrn;
        final Urn searchPlaylistsUrn = screenDataMap.get(Screen.SEARCH_PLAYLISTS).queryUrn;
        final Urn searchUsersUrn = screenDataMap.get(Screen.SEARCH_USERS).queryUrn;
        assertThat(searchEverythingUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchTracksUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);

        final boolean searchEverythingHasPremiumContent = screenDataMap.get(Screen.SEARCH_EVERYTHING).hasPremiumContent;
        final boolean searchTracksUrnHasPremiumContent = screenDataMap.get(Screen.SEARCH_TRACKS).hasPremiumContent;
        final boolean searchPlaylistsUrnHasPremiumContent = screenDataMap.get(Screen.SEARCH_PLAYLISTS).hasPremiumContent;
        final boolean searchUsersUrnHasPremiumContent = screenDataMap.get(Screen.SEARCH_USERS).hasPremiumContent;
        assertThat(searchEverythingHasPremiumContent).isFalse();
        assertThat(searchTracksUrnHasPremiumContent).isFalse();
        assertThat(searchPlaylistsUrnHasPremiumContent).isFalse();
        assertThat(searchUsersUrnHasPremiumContent).isFalse();
    }
}
