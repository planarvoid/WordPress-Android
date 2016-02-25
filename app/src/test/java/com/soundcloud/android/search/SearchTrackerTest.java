package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Before
    public void setUp() {
        tracker = new SearchTracker(eventBus);
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
        tracker.trackSearchSubmission(TYPE_ALL, Optional.of(QUERY_URN));

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_EVERYTHING));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(QUERY_URN));
    }

    @Test
    public void mustNotTrackSearchSubmissionWithoutQueryUrn() {
        tracker.trackSearchSubmission(TYPE_ALL, Optional.<Urn>absent());

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustSetQueryUrnForScreen() {
        tracker.setQueryUrnForSearchType(TYPE_ALL, Optional.of(QUERY_URN));
        tracker.setQueryUrnForSearchType(TYPE_TRACKS, Optional.of(QUERY_URN));

        final Map<Screen, Urn> screenQueryUrnMap = tracker.getScreenQueryUrnMap();
        final Urn searchEverythingUrn = screenQueryUrnMap.get(Screen.SEARCH_EVERYTHING);
        final Urn searchTracksUrn = screenQueryUrnMap.get(Screen.SEARCH_TRACKS);
        final Urn searchPlaylistsUrn = screenQueryUrnMap.get(Screen.SEARCH_PLAYLISTS);
        final Urn searchUsersUrn = screenQueryUrnMap.get(Screen.SEARCH_USERS);

        assertThat(searchEverythingUrn).isEqualTo(QUERY_URN);
        assertThat(searchTracksUrn).isEqualTo(QUERY_URN);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void mustTrackResultsScreenEventWithValidSearchScreen() {
        tracker.setQueryUrnForSearchType(TYPE_ALL, Optional.of(QUERY_URN));
        tracker.trackResultsScreenEvent(TYPE_ALL);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_TRACKS));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(QUERY_URN));
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidScreen() {
        tracker.setQueryUrnForSearchType(TYPE_TRACKS, Optional.of(QUERY_URN));
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

        tracker.setQueryUrnForSearchType(TYPE_ALL, Optional.of(QUERY_URN));
        tracker.setQueryUrnForSearchType(TYPE_TRACKS, Optional.of(QUERY_URN));

        final Map<Screen, Urn> screenQueryUrnMap = tracker.getScreenQueryUrnMap();
        assertThat(screenQueryUrnMap.get(Screen.SEARCH_EVERYTHING)).isEqualTo(QUERY_URN);
        assertThat(screenQueryUrnMap.get(Screen.SEARCH_TRACKS)).isEqualTo(QUERY_URN);

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

    private void assertTrackerInitialState() {
        final Map<Screen, Urn> screenQueryUrnMap = tracker.getScreenQueryUrnMap();
        final Urn searchEverythingUrn = screenQueryUrnMap.get(Screen.SEARCH_EVERYTHING);
        final Urn searchTracksUrn = screenQueryUrnMap.get(Screen.SEARCH_TRACKS);
        final Urn searchPlaylistsUrn = screenQueryUrnMap.get(Screen.SEARCH_PLAYLISTS);
        final Urn searchUsersUrn = screenQueryUrnMap.get(Screen.SEARCH_USERS);

        assertThat(screenQueryUrnMap.size()).isEqualTo(SEARCH_RESULT_TYPES);
        assertThat(searchEverythingUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchTracksUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
    }
}
