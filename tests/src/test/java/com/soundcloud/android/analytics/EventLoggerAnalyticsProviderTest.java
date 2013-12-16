package com.soundcloud.android.analytics;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.analytics.eventlogger.EventLogger;
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
        final PlaybackEventData mock = Mockito.mock(PlaybackEventData.class);
        eventLoggerAnalyticsProvider.trackPlaybackEvent(mock);
        verify(eventLogger).trackEvent(mock);
    }

    @Module(library = true, injects = EventLoggerAnalyticsProviderTest.class)
    public class TestModule {
        @Provides
        public EventLogger provideEventLogger(){
            return eventLogger;
        }
    }
}
