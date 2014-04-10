package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.Protocol;
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
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, Protocol.HLS, PlayerType.MEDIA_PLAYER, "uri");
        when(eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(event)).thenReturn("event-params");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        ArgumentCaptor<EventLoggerEvent> captor = ArgumentCaptor.forClass(EventLoggerEvent.class);
        verify(eventLogger).trackEvent(captor.capture());
        expect(captor.getValue().getParams()).toEqual("event-params");
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimeStamp());
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
