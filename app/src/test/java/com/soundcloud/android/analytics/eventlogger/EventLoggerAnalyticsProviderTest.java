package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.MidTierTrackEvent;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.SharedPreferences;

import java.util.List;

public class EventLoggerAnalyticsProviderTest extends AndroidUnitTest {

    private EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock private EventTracker eventTracker;
    @Mock private EventLoggerJsonDataBuilder dataBuilderv0;
    @Mock private EventLoggerV1JsonDataBuilder dataBuilderv1;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private FeatureFlags featureFlags;

    private Urn userUrn = Urn.forUser(123L);
    private TrackSourceInfo trackSourceInfo;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() throws Exception {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTracker, InjectionSupport.lazyOf(dataBuilderv0),
                InjectionSupport.lazyOf(dataBuilderv1), sharedPreferences, featureFlags);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:clicked:urn"));
    }

    @Test
    public void shouldTrackPlaybackEventAtStartOfAdTrackAsAdImpression() throws Exception {
        PlaybackSessionEvent event = mock(PlaybackSessionEvent.class);
        when(event.isAd()).thenReturn(true);
        when(event.isFirstPlay()).thenReturn(true);
        when(event.getTimestamp()).thenReturn(12345L);
        when(dataBuilderv0.buildForAudioAdImpression(event)).thenReturn("adUrl");
        when(dataBuilderv0.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues).hasSize(2);

        TrackingRecord adEvent = allValues.get(0);
        assertThat(adEvent.getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(adEvent.getTimeStamp()).isEqualTo(12345L);
        assertThat(adEvent.getData()).isEqualTo("adUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAtEndOfAdTrackAsAdFinishClick() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);
        when(dataBuilderv0.buildForAdFinished(event)).thenReturn("clickUrl");
        when(dataBuilderv0.buildForAudioEvent(event)).thenReturn("audioEventUrl");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);

        TrackingRecord adEvent = allValues.get(0);
        assertThat(adEvent.getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(adEvent.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(adEvent.getData()).isEqualTo("clickUrl");
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(dataBuilderv0.buildForAudioEvent(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getValue().getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(captor.getValue().getData()).isEqualTo("url");
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HLS, PlayerType.MEDIA_PLAYER,
                ConnectionType.FOUR_G, "uri", userUrn);
        when(dataBuilderv0.build(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getValue().getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(captor.getValue().getData()).isEqualTo("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() throws Exception {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category", PlaybackProtocol.HLS, "uri", "bitrate", "format", ConnectionType.FOUR_G);
        when(dataBuilderv0.build(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getValue().getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(captor.getValue().getData()).isEqualTo("url");
    }

    @Test
    public void shouldTrackAudioAdRelatedUIEvents() {
        UIEvent event1 = UIEvent.fromAudioAdClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456), userUrn, trackSourceInfo);
        UIEvent event2 = UIEvent.fromAudioAdCompanionDisplayClick(TestPropertySets.audioAdProperties(Urn.forTrack(123)), Urn.forTrack(456), userUrn, trackSourceInfo, 1000);
        when(dataBuilderv0.build(event1)).thenReturn("url1");
        when(dataBuilderv0.build(event2)).thenReturn("url2");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event1);
        eventLoggerAnalyticsProvider.handleTrackingEvent(event2);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0).getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getAllValues().get(0).getTimeStamp()).isEqualTo(event1.getTimestamp());
        assertThat(captor.getAllValues().get(0).getData()).isEqualTo("url1");
        assertThat(captor.getAllValues().get(1).getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getAllValues().get(1).getTimeStamp()).isEqualTo(event2.getTimestamp());
        assertThat(captor.getAllValues().get(1).getData()).isEqualTo("url2");
    }

    @Test
    public void shouldTrackLikeEvents() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        UIEvent event = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", "page_name", Urn.forTrack(123), Urn.NOT_SET, promotedSourceInfo, PlayableMetadata.EMPTY);

        when(dataBuilderv0.build(event)).thenReturn("ForLikeEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForLikeEvent");
    }

    @Test
    public void shouldTrackUnlikeEvents() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        UIEvent event = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", "page_name", Urn.forTrack(123), Urn.NOT_SET, promotedSourceInfo, PlayableMetadata.EMPTY);

        when(dataBuilderv0.build(event)).thenReturn("ForUnlikeEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnlikeEvent");
    }

    @Test
    public void shouldTrackRepostEvents() {
        PropertySet trackProperties = TestPropertySets.expectedPromotedTrack();
        PromotedListItem promotedTrack = PromotedTrackItem.from(trackProperties);
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        UIEvent event = UIEvent.fromToggleRepost(true, "screen", "page_name", Urn.forTrack(123), Urn.NOT_SET, promotedSourceInfo, PlayableMetadata.from(trackProperties));

        when(dataBuilderv0.build(event)).thenReturn("ForRepostEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForRepostEvent");
    }

    @Test
    public void shouldTrackUnRepostEvents() {
        PropertySet trackProperties = TestPropertySets.expectedPromotedTrack();
        PromotedListItem promotedTrack = PromotedTrackItem.from(trackProperties);
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        UIEvent event = UIEvent.fromToggleRepost(false, "screen", "page_name", Urn.forTrack(123), Urn.NOT_SET, promotedSourceInfo, PlayableMetadata.from(trackProperties));

        when(dataBuilderv0.build(event)).thenReturn("ForUnRepostEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnRepostEvent");
    }

    @Test
    public void shouldTrackLeaveBehindImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(dataBuilderv0.build(event)).thenReturn("ForAudioAdImpression");
        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForAudioAdImpression");
    }

    @Test
    public void shouldTrackLeaveBehindClickTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(dataBuilderv0.build(event)).thenReturn("ForAudioAdClick");
        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForAudioAdClick");
    }

    @Test
    public void shouldTrackPromotedTrackEvents() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent event = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");
        when(dataBuilderv0.build(event)).thenReturn("ForPromotedEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForPromotedEvent");
    }

    @Test
    public void shouldTrackMidTierTrackEvent() {
        MidTierTrackEvent event = MidTierTrackEvent.forImpression(Urn.forTrack(123), "screen_tag");
        when(dataBuilderv0.build(event)).thenReturn("ForMidTierTrackEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForMidTierTrackEvent");
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
        when(dataBuilderv0.build(event)).thenReturn("ForScreenEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForScreenEvent");
    }

    @Test
    public void shouldTrackUpsellEvent() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forNavClick();
        when(dataBuilderv0.build(event)).thenReturn("ForUpsellEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUpsellEvent");
    }

    @Test
    public void shouldTrackSearchSuggestionSearchEvents() {
        SearchEvent event = SearchEvent.searchSuggestion(Content.SEARCH, false, searchQuerySourceInfo);
        assertThat(searchEventUrlCaptor("ForSearchEvent", event)).isEqualTo("ForSearchEvent");
    }

    @Test
    public void shouldTrackTapTrackOnScreenSearchEvents() {
        SearchEvent event = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEventUrlCaptor("ForSearchEvent", event)).isEqualTo("ForSearchEvent");
    }

    @Test
    public void shouldTrackTapUserOnScreenSearchEvents() {
        SearchEvent event = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEventUrlCaptor("ForSearchEvent", event)).isEqualTo("ForSearchEvent");
    }

    @Test
    public void shouldTrackSearchStartSearchEvents() {
        SearchEvent event = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEventUrlCaptor("ForSearchEvent", event)).isEqualTo("ForSearchEvent");
    }

    @Test
    public void shouldTrackLikesToOfflineEvent() {
        UIEvent event = UIEvent.fromAddOfflineLikes("page_name");
        assertThat(v1UIEventCaptor("ForOfflineLikesEvent", event)).isEqualTo("ForOfflineLikesEvent");
    }

    @Test
    public void shouldTrackCollectionToOfflineEvent() {
        UIEvent event = UIEvent.fromToggleOfflineCollection(true);
        assertThat(v1UIEventCaptor("ForOfflineCollection", event)).isEqualTo("ForOfflineCollection");
    }

    @Test
    public void shouldTrackPlaylistToOfflineEvent() {
        UIEvent event = UIEvent.fromAddOfflinePlaylist("page_name", Urn.forPlaylist(123L), null);
        assertThat(v1UIEventCaptor("ForOfflinePlaylistEvent", event)).isEqualTo("ForOfflinePlaylistEvent");
    }

    @Test
    public void shouldForwardFlushCallToEventTracker() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventTracker).flush(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
    }

    private String v1UIEventCaptor(String name, UIEvent event) {
        when(dataBuilderv1.buildForUIEvent(event)).thenReturn(name);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private String searchEventUrlCaptor(String name, SearchEvent event) {
        when(dataBuilderv0.build(event)).thenReturn(name);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        return captor.getValue().getData();
    }
}
