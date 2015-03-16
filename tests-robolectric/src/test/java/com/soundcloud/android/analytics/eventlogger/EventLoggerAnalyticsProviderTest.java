package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
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
    @Mock private EventLoggerDataBuilder eventLoggerDataBuilder;
    @Mock private FeatureFlags featureFlags;

    private Urn userUrn = Urn.forUser(123L);
    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTracker, eventLoggerDataBuilder, featureFlags);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
    }

    @Test
    public void shouldTrackPlaybackEventAtStartOfAdTrackAsAdImpression() throws Exception {
        PlaybackSessionEvent event = mock(PlaybackSessionEvent.class);
        when(event.isAd()).thenReturn(true);
        when(event.isFirstPlay()).thenReturn(true);
        when(event.getTimeStamp()).thenReturn(12345L);
        when(eventLoggerDataBuilder.buildForAudioAdImpression(event)).thenReturn("adUrl");
        when(eventLoggerDataBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(12345L);
        expect(adEvent.getUrl()).toEqual("adUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAtEndOfAdTrackAsAdFinishClick() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);
        when(eventLoggerDataBuilder.buildForAdFinished(event)).thenReturn("clickUrl");
        when(eventLoggerDataBuilder.buildForAudioEvent(event)).thenReturn("audioEventUrl");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(event.getTimeStamp());
        expect(adEvent.getUrl()).toEqual("clickUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(eventLoggerDataBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimeStamp());
        expect(captor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HLS, PlayerType.MEDIA_PLAYER,
                ConnectionType.FOUR_G, "uri", userUrn);
        when(eventLoggerDataBuilder.build(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimeStamp());
        expect(captor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() throws Exception {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category", PlaybackProtocol.HLS, "uri", "bitrate", "format", ConnectionType.FOUR_G);
        when(eventLoggerDataBuilder.build(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimestamp());
        expect(captor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldTrackAudioAdRelatedUIEvents() {
        UIEvent event1 = UIEvent.fromAudioAdClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456), userUrn, trackSourceInfo);
        UIEvent event2 = UIEvent.fromAudioAdCompanionDisplayClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456), userUrn, trackSourceInfo, 1000);
        when(eventLoggerDataBuilder.build(event1)).thenReturn("url1");
        when(eventLoggerDataBuilder.build(event2)).thenReturn("url2");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event1);
        eventLoggerAnalyticsProvider.handleTrackingEvent(event2);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());
        expect(captor.getAllValues()).toNumber(2);
        expect(captor.getAllValues().get(0).getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getAllValues().get(0).getTimeStamp()).toEqual(event1.getTimeStamp());
        expect(captor.getAllValues().get(0).getUrl()).toEqual("url1");
        expect(captor.getAllValues().get(1).getBackend()).toEqual(EventLoggerAnalyticsProvider.BACKEND_NAME);
        expect(captor.getAllValues().get(1).getTimeStamp()).toEqual(event2.getTimeStamp());
        expect(captor.getAllValues().get(1).getUrl()).toEqual("url2");
    }

    @Test
    public void shouldTrackLeaveBehindImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(eventLoggerDataBuilder.build(event)).thenReturn("ForAudioAdImpression");
        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        captor.getValue().getUrl().equals("ForAudioAdImpression");
    }

    @Test
    public void shouldTrackLeaveBehindClickTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(eventLoggerDataBuilder.build(event)).thenReturn("ForAudioAdClick");
        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        captor.getValue().getUrl().equals("ForAudioAdClick");
    }

    @Test
    public void shouldNotTrackOtherUIEvents() {
        UIEvent event = new UIEvent(UIEvent.KIND_NAVIGATION);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void shouldTrackScreenEvent() {
        when(featureFlags.isEnabled(Flag.EVENTLOGGER_PAGE_VIEW_EVENTS)).thenReturn(true);
        ScreenEvent event = ScreenEvent.create(Screen.ACTIVITIES);
        when(eventLoggerDataBuilder.build(event)).thenReturn("ForScreenEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        captor.getValue().getUrl().equals("ForScreenEvent");
    }

    @Test
    public void shouldForwardFlushCallToEventTracker() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventTracker).flush(EventLoggerAnalyticsProvider.BACKEND_NAME);
    }
}
