package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

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
    public void shouldTrackPlaybackEventInPlayEventTracker(){
        final PlaybackEvent mock = Mockito.mock(PlaybackEvent.class);
        eventLoggerAnalyticsProvider.handlePlaybackEvent(mock);
        verify(eventLogger).trackEvent(mock);
    }

    @Test
    public void shouldForwardFlushCallToEventLogger() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventLogger).flush();
    }

    @Module(library = true, injects = EventLoggerAnalyticsProviderTest.class)
    public class TestModule {
        @Provides
        public EventLogger provideEventLogger(){
            return eventLogger;
        }
    }
}
