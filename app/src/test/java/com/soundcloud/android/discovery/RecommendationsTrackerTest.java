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
public class RecommendationsTrackerTest {

    private RecommendationsTracker tracker;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        tracker = new RecommendationsTracker(eventBus);
    }

    @Test
    public void shouldTrackPageViewEvent() {
        tracker.trackPageViewEvent();

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(event.getKind().equals(Screen.RECOMMENDATIONS_MAIN));
    }
}
