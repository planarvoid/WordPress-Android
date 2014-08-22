package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestEvents;
import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
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
        when(event.isFirstPlay()).thenReturn(true);
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
    public void shouldTrackPlaybackEventAtEndOfAdTrackAsAdFinishClick() throws Exception {
        PropertySet audioAd = TestPropertySets.expectedAudioAdForAnalytics(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);
        when(eventLoggerUrlBuilder.buildForAdFinished(event)).thenReturn("clickUrl");
        when(eventLoggerUrlBuilder.buildForAudioEvent(event)).thenReturn("audioEventUrl");

        eventLoggerAnalyticsProvider.handlePlaybackSessionEvent(event);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingEvent> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingEvent adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(event.getTimeStamp());
        expect(adEvent.getUrl()).toEqual("clickUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(eventLoggerUrlBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackSessionEvent(event);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimeStamp());
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
    public void shouldTrackUIEvents() {
        UIEvent event = UIEvent.fromAudioAdClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456));
        when(eventLoggerUrlBuilder.buildForClick(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleUIEvent(event);

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
