package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerAnalyticsProviderTest {

    private EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock
    private EventLogger eventLogger;
    @Mock
    private EventLoggerParamsBuilder eventLoggerParamsBuilder;

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventLogger, eventLoggerParamsBuilder);
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackEvent event = PlaybackEvent.forPlay(new Track(1), 123l, new TrackSourceInfo("context", false), 1000L);
        when(eventLoggerParamsBuilder.buildFromPlaybackEvent(event)).thenReturn("event-params");

        eventLoggerAnalyticsProvider.handlePlaybackEvent(event);

        ArgumentCaptor<EventLoggerEvent> captor = ArgumentCaptor.forClass(EventLoggerEvent.class);
        verify(eventLogger).trackEvent(captor.capture());
        expect(captor.getValue().getParams()).toEqual("event-params");
        expect(captor.getValue().getTimeStamp()).toEqual(1000L);
        expect(captor.getValue().getPath()).toEqual("audio");
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() throws Exception {
        PlaybackPerformanceEvent event = Mockito.mock(PlaybackPerformanceEvent.class);
        when(event.getTimeStamp()).thenReturn(1000L);
        when(eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(event)).thenReturn("event-params");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        ArgumentCaptor<EventLoggerEvent> captor = ArgumentCaptor.forClass(EventLoggerEvent.class);
        verify(eventLogger).trackEvent(captor.capture());
        expect(captor.getValue().getParams()).toEqual("event-params");
        expect(captor.getValue().getTimeStamp()).toEqual(1000L);
        expect(captor.getValue().getPath()).toEqual("audio_performance");
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
