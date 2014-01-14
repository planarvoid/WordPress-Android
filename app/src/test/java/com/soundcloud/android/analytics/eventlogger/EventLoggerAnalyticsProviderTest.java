package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerAnalyticsProviderTest {

    EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock
    EventLogger eventLogger;
    @Mock
    ObjectGraph objectGraph;

    @Before
    public void setUp() throws Exception {
        MockInjector dependencyInjector = new MockInjector(new TestModule());
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(dependencyInjector);
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

    @Module(library = true, injects = EventLoggerAnalyticsProviderTest.class)
    public class TestModule {
        @Provides
        public EventLogger provideEventLogger(){
            return eventLogger;
        }
    }
}
