package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static final Urn queryUrn = Urn.forTrack(123L);
    private static final int searchResultTypes = 4;

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
        tracker.trackSearchSubmission(Screen.SEARCH_EVERYTHING, Optional.of(queryUrn));

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_EVERYTHING));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(queryUrn));
    }

    @Test
    public void mustNotTrackSearchSubmissionWithoutQueryUrn() {
        tracker.trackSearchSubmission(Screen.SEARCH_EVERYTHING, Optional.<Urn>absent());

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustSetQueryUrnForScreen() {
        tracker.setQueryUrnForScreen(Screen.SEARCH_EVERYTHING, Optional.of(queryUrn));
        tracker.setQueryUrnForScreen(Screen.SEARCH_TRACKS, Optional.of(queryUrn));

        final Map<Screen, Urn> screenQueryUrnMap = tracker.getScreenQueryUrnMap();
        final Urn searchEverythingUrn = screenQueryUrnMap.get(Screen.SEARCH_EVERYTHING);
        final Urn searchTracksUrn = screenQueryUrnMap.get(Screen.SEARCH_TRACKS);
        final Urn searchPlaylistsUrn = screenQueryUrnMap.get(Screen.SEARCH_PLAYLISTS);
        final Urn searchUsersUrn = screenQueryUrnMap.get(Screen.SEARCH_USERS);

        assertThat(searchEverythingUrn).isEqualTo(queryUrn);
        assertThat(searchTracksUrn).isEqualTo(queryUrn);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void mustTrackResultsScreenEventWithValidSearchScreen() {
        tracker.setQueryUrnForScreen(Screen.SEARCH_TRACKS, Optional.of(queryUrn));
        tracker.trackResultsScreenEvent(Screen.SEARCH_TRACKS);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.SEARCH_TRACKS));
        assertThat(event.get(SearchEvent.KEY_QUERY_URN).equals(queryUrn));
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidScreen() {
        tracker.setQueryUrnForScreen(Screen.SEARCH_TRACKS, Optional.of(queryUrn));
        tracker.trackResultsScreenEvent(Screen.ACTIVITIES);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustNotTrackResultsScreenEventWithInvalidQueryUrnNotSet() {
        tracker.trackResultsScreenEvent(Screen.SEARCH_EVERYTHING);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(0);
    }

    @Test
    public void mustNotSetQueryUrnForUnknownScreenOrAbsentQueryUrn() {
        tracker.setQueryUrnForScreen(Screen.ACTIVITIES, Optional.of(queryUrn));
        tracker.setQueryUrnForScreen(Screen.SEARCH_EVERYTHING, Optional.<Urn>absent());

        assertTrackerInitialState();
    }

    @Test
    public void mustResetTrackingState() {
        assertTrackerInitialState();

        tracker.setQueryUrnForScreen(Screen.SEARCH_EVERYTHING, Optional.of(queryUrn));
        tracker.setQueryUrnForScreen(Screen.SEARCH_TRACKS, Optional.of(queryUrn));

        final Map<Screen, Urn> screenQueryUrnMap = tracker.getScreenQueryUrnMap();
        assertThat(screenQueryUrnMap.get(Screen.SEARCH_EVERYTHING)).isEqualTo(queryUrn);
        assertThat(screenQueryUrnMap.get(Screen.SEARCH_TRACKS)).isEqualTo(queryUrn);

        tracker.reset();

        assertTrackerInitialState();
    }

    private void assertTrackerInitialState() {
        final Map<Screen, Urn> screenQueryUrnMap = tracker.getScreenQueryUrnMap();
        final Urn searchEverythingUrn = screenQueryUrnMap.get(Screen.SEARCH_EVERYTHING);
        final Urn searchTracksUrn = screenQueryUrnMap.get(Screen.SEARCH_TRACKS);
        final Urn searchPlaylistsUrn = screenQueryUrnMap.get(Screen.SEARCH_PLAYLISTS);
        final Urn searchUsersUrn = screenQueryUrnMap.get(Screen.SEARCH_USERS);

        assertThat(screenQueryUrnMap.size()).isEqualTo(searchResultTypes);
        assertThat(searchEverythingUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchTracksUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchPlaylistsUrn).isEqualTo(Urn.NOT_SET);
        assertThat(searchUsersUrn).isEqualTo(Urn.NOT_SET);
    }
}
