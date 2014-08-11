package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static org.mockito.Mockito.*;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.PropertySets;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerAnalyticsProviderTest {

    private EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock private EventTracker eventTracker;
    @Mock private EventLoggerUrlBuilder eventLoggerUrlBuilder;

    private UserUrn userUrn = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTracker, eventLoggerUrlBuilder);
    }

    @Test
    public void shouldTrackPlaybackEventAtStartOfAdTrackAsAdImpression() throws Exception {
        PlaybackSessionEvent event = mock(PlaybackSessionEvent.class);
        when(event.isAd()).thenReturn(true);
        when(event.isAtStart()).thenReturn(true);
        when(event.getTimeStamp()).thenReturn(12345L);
        when(eventLoggerUrlBuilder.buildForAdImpression(event)).thenReturn("adUrl");
        when(eventLoggerUrlBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackSessionEvent(event);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingEvent> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingEvent adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(12345L);
        expect(adEvent.getUrl()).toEqual("adUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        final PropertySet track = PropertySets.expectedTrackDataForAnalytics(Urn.forTrack(1L), "allow", 1000);
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, Urn.forUser(123L),
                new TrackSourceInfo("context", false), 0L, 12345L);
        when(eventLoggerUrlBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackSessionEvent(event);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(12345L);
        expect(captor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HLS, PlayerType.MEDIA_PLAYER,
                ConnectionType.FOUR_G, "uri", userUrn);
        when(eventLoggerUrlBuilder.buildForAudioPerformanceEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimeStamp());
        expect(captor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() throws Exception {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category", PlaybackProtocol.HLS, "uri", "bitrate", "format");
        when(eventLoggerUrlBuilder.buildForAudioErrorEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimestamp());
        expect(captor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldForwardFlushCallToEventLogger() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventTracker).flush(EventLoggerAnalyticsProvider.BACKEND_NAME);
    }
}
