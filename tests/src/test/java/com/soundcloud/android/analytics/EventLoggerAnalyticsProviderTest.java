package com.soundcloud.android.analytics;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracking.eventlogger.EventLogger;
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

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider();
    }

    @Test
    public void shouldTrackPlaybackEventInPlayEventTracker(){
        final PlaybackEventData mock = Mockito.mock(PlaybackEventData.class);
        eventLoggerAnalyticsProvider.trackPlaybackEvent(mock);
        verify(eventLogger).trackEvent(mock);
    }
}
