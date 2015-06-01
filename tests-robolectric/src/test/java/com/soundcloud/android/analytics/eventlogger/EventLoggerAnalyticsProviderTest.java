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
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.PromotedTrackEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.SharedPreferences;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerAnalyticsProviderTest {

    private EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock private EventTracker eventTracker;
    @Mock private EventLoggerJsonDataBuilder dataBuilder;
    @Mock private SharedPreferences sharedPreferences;

    private Urn userUrn = Urn.forUser(123L);
    private TrackSourceInfo trackSourceInfo;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTracker, dataBuilder, sharedPreferences);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:clicked:urn"));
    }

    @Test
    public void shouldTrackPlaybackEventAtStartOfAdTrackAsAdImpression() throws Exception {
        PlaybackSessionEvent event = mock(PlaybackSessionEvent.class);
        when(event.isAd()).thenReturn(true);
        when(event.isFirstPlay()).thenReturn(true);
        when(event.getTimestamp()).thenReturn(12345L);
        when(dataBuilder.buildForAudioAdImpression(event)).thenReturn("adUrl");
        when(dataBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(12345L);
        expect(adEvent.getData()).toEqual("adUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAtEndOfAdTrackAsAdFinishClick() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);
        when(dataBuilder.buildForAdFinished(event)).thenReturn("clickUrl");
        when(dataBuilder.buildForAudioEvent(event)).thenReturn("audioEventUrl");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(event.getTimestamp());
        expect(adEvent.getData()).toEqual("clickUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(dataBuilder.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimestamp());
        expect(captor.getValue().getData()).toEqual("url");
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HLS, PlayerType.MEDIA_PLAYER,
                ConnectionType.FOUR_G, "uri", userUrn);
        when(dataBuilder.build(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimeStamp());
        expect(captor.getValue().getData()).toEqual("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() throws Exception {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category", PlaybackProtocol.HLS, "uri", "bitrate", "format", ConnectionType.FOUR_G);
        when(dataBuilder.build(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(captor.getValue().getTimeStamp()).toEqual(event.getTimestamp());
        expect(captor.getValue().getData()).toEqual("url");
    }

    @Test
    public void shouldTrackAudioAdRelatedUIEvents() {
        UIEvent event1 = UIEvent.fromAudioAdClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456), userUrn, trackSourceInfo);
        UIEvent event2 = UIEvent.fromAudioAdCompanionDisplayClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456), userUrn, trackSourceInfo, 1000);
        when(dataBuilder.build(event1)).thenReturn("url1");
        when(dataBuilder.build(event2)).thenReturn("url2");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event1);
        eventLoggerAnalyticsProvider.handleTrackingEvent(event2);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());
        expect(captor.getAllValues()).toNumber(2);
        expect(captor.getAllValues().get(0).getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(captor.getAllValues().get(0).getTimeStamp()).toEqual(event1.getTimestamp());
        expect(captor.getAllValues().get(0).getData()).toEqual("url1");
        expect(captor.getAllValues().get(1).getBackend()).toEqual(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(captor.getAllValues().get(1).getTimeStamp()).toEqual(event2.getTimestamp());
        expect(captor.getAllValues().get(1).getData()).toEqual("url2");
    }

    @Test
    public void shouldTrackLeaveBehindImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(dataBuilder.build(event)).thenReturn("ForAudioAdImpression");
        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getData()).toEqual("ForAudioAdImpression");
    }

    @Test
    public void shouldTrackLeaveBehindClickTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(dataBuilder.build(event)).thenReturn("ForAudioAdClick");
        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getData()).toEqual("ForAudioAdClick");
    }

    @Test
    public void shouldTrackPromotedTrackEvents() {
        PromotedTrackItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackEvent event = PromotedTrackEvent.forPromoterClick(promotedTrack, "stream");
        when(dataBuilder.build(event)).thenReturn("ForPromotedEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getData()).toEqual("ForPromotedEvent");
    }

    @Test
    public void shouldNotTrackOtherUIEvents() {
        UIEvent event = new UIEvent(UIEvent.KIND_NAVIGATION);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void shouldTrackScreenEvent() {
        ScreenEvent event = ScreenEvent.create(Screen.ACTIVITIES);
        when(dataBuilder.build(event)).thenReturn("ForScreenEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        expect(captor.getValue().getData()).toEqual("ForScreenEvent");
    }

    @Test
    public void shouldTrackSearchSuggestionSearchEvents() {
        SearchEvent event = SearchEvent.searchSuggestion(Content.SEARCH, false, searchQuerySourceInfo);
        expect(searchEventUrlCaptor("ForSearchEvent", event)).toEqual("ForSearchEvent");
    }

    @Test
    public void shouldTrackTapTrackOnScreenSearchEvents() {
        SearchEvent event = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEventUrlCaptor("ForSearchEvent", event)).toEqual("ForSearchEvent");
    }

    @Test
    public void shouldTrackTapUserOnScreenSearchEvents() {
        SearchEvent event = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEventUrlCaptor("ForSearchEvent", event)).toEqual("ForSearchEvent");
    }

    @Test
    public void shouldTrackSearchStartSearchEvents() {
        SearchEvent event = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEventUrlCaptor("ForSearchEvent", event)).toEqual("ForSearchEvent");
    }

    @Test
    public void shouldForwardFlushCallToEventTracker() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventTracker).flush(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
    }

    private String searchEventUrlCaptor(String name, SearchEvent event) {
        when(dataBuilder.build(event)).thenReturn(name);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        return captor.getValue().getData();
    }
}
