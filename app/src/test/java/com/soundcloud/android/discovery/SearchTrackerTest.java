package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchTrackerTest {

    private SearchTracker tracker;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        tracker = new SearchTracker(eventBus);
    }

    @Test
    public void mustTrackSearchScreen() {
        tracker.trackScreenEvent();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind().equals(Screen.SEARCH_MAIN));
    }
}
