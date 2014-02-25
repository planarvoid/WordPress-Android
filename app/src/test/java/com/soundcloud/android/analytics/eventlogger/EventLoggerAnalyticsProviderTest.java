package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerAnalyticsProviderTest {

    private EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock
    private EventLogger eventLogger;

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventLogger);
    }

    @Test
    public void shouldTrackPlaybackEventInPlayEventTracker() throws CreateModelException {
        final PlaybackEvent event = TestHelper.getModelFactory().createModel(PlaybackEvent.class);
        eventLoggerAnalyticsProvider.handlePlaybackEvent(event);
        verify(eventLogger).trackEvent(event);
    }

    @Test
    public void shouldForwardFlushCallToEventLogger() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventLogger).flush();
    }

    @Test
    public void shouldStopEventLoggerOnPlayerLifeCycleDestroyedEvent() {
        PlayerLifeCycleEvent event = PlayerLifeCycleEvent.forDestroyed();
        eventLoggerAnalyticsProvider.handlePlayerLifeCycleEvent(event);
        verify(eventLogger).stop();
    }
}
