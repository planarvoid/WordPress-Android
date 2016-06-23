package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdDeliveryEvent.AdsReceived;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.skippy.Skippy;
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

    private Urn userUrn = Urn.forUser(123L);
    private Urn trackUrn = Urn.forTrack(123L);

    private TrackSourceInfo trackSourceInfo;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTracker,
                                                                        InjectionSupport.lazyOf(dataBuilderv0),
                                                                        InjectionSupport.lazyOf(dataBuilderv1),
                                                                        sharedPreferences);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:clicked:urn"));
    }

    @Test
    public void shouldTrackPlaybackEventAtStartOfAdTrackAsAdImpression() {
        PlaybackSessionEvent event = mock(PlaybackSessionEvent.class);
        when(event.isAd()).thenReturn(true);
        when(event.shouldReportAdStart()).thenReturn(true);
        when(event.getTimestamp()).thenReturn(12345L);
        when(dataBuilderv0.buildForAudioAdImpression(event)).thenReturn("impressionUrl");
        when(dataBuilderv1.buildForAudioEvent(event)).thenReturn("audioEventUrl");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues).hasSize(2);
        assertEventTracked(allValues.get(0), "impressionUrl", event.getTimestamp());
        assertEventTracked(allValues.get(1), "audioEventUrl", event.getTimestamp());
    }

    @Test
    public void shouldTrackPlaybackEventAtEndOfAdTrackAsAdFinishClick() {
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);
        when(dataBuilderv0.buildForAdFinished(event)).thenReturn("clickUrl");
        when(dataBuilderv1.buildForAudioEvent(event)).thenReturn("audioEventUrl");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);
        assertEventTracked(captor.getAllValues().get(0), "clickUrl", event.getTimestamp());
        assertEventTracked(captor.getAllValues().get(1), "audioEventUrl", event.getTimestamp());
    }

    @Test
    public void shouldTrackPlaybackEventAtStartOfAdTrackAsAdImpressionForThirdPartyAd() {
        AudioAd audioAd = AdFixtures.getThirdPartyAudioAd(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEventWithProgress(0,
                                                                                     AdConstants.THIRD_PARTY_AD_MAGIC_TRACK_URN)
                                               .withAudioAd(audioAd);
        when(dataBuilderv0.buildForAudioAdImpression(event)).thenReturn("impressionUrl");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());
        assertThat(captor.getAllValues().size()).isEqualTo(1);
        assertEventTracked(captor.getValue(), "impressionUrl", event.getTimestamp());
        verify(dataBuilderv1, never()).buildForAudioEvent(event);
    }

    @Test
    public void shouldTrackPlaybackEventAtEndOfAdTrackAsAdFinishClickForThirdPartyAd() {
        AudioAd audioAd = AdFixtures.getThirdPartyAudioAd(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent(AdConstants.THIRD_PARTY_AD_MAGIC_TRACK_URN)
                                               .withAudioAd(audioAd);
        when(dataBuilderv0.buildForAdFinished(event)).thenReturn("clickUrl");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker, atLeastOnce()).trackEvent(captor.capture());
        assertThat(captor.getAllValues().size()).isEqualTo(1);
        assertEventTracked(captor.getValue(), "clickUrl", event.getTimestamp());
        verify(dataBuilderv1, never()).buildForAudioEvent(event);
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(dataBuilderv1.buildForAudioEvent(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L,
                                                                             PlaybackProtocol.HLS,
                                                                             PlayerType.MEDIA_PLAYER,
                                                                             ConnectionType.FOUR_G,
                                                                             "uri",
                                                                             Skippy.SkippyMediaType.UNKNOWN.name(),
                                                                             0,
                                                                             userUrn,
                                                                             false);
        when(dataBuilderv0.build(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    public void shouldTrackRichMediaPlaybackPerformanceEventAsEventLoggerEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L,
                                                                             PlaybackProtocol.HLS,
                                                                             PlayerType.MEDIA_PLAYER,
                                                                             ConnectionType.FOUR_G,
                                                                             "uri",
                                                                             "video/mp4",
                                                                             200,
                                                                             userUrn,
                                                                             true);
        when(dataBuilderv1.buildForRichMediaPerformance(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(dataBuilderv0, never()).build(event);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getValue().getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(captor.getValue().getData()).isEqualTo("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category",
                                                          PlaybackProtocol.HLS,
                                                          "uri",
                                                          Skippy.SkippyMediaType.MP3.name(),
                                                          128000,
                                                          ConnectionType.FOUR_G);
        when(dataBuilderv0.build(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    @Test
    public void shouldTrackAudioAdRelatedUIEvents() {
        UIEvent event1 = UIEvent.fromAudioAdClick(AdFixtures.getAudioAd(Urn.forTrack(123L)),
                                                  Urn.forTrack(456),
                                                  userUrn,
                                                  trackSourceInfo);
        UIEvent event2 = UIEvent.fromAudioAdCompanionDisplayClick(AdFixtures.getAudioAd(Urn.forTrack(123L)),
                                                                  Urn.forTrack(456),
                                                                  userUrn,
                                                                  trackSourceInfo,
                                                                  1000);
        when(dataBuilderv0.build(event1)).thenReturn("url1");
        when(dataBuilderv0.build(event2)).thenReturn("url2");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event1);
        eventLoggerAnalyticsProvider.handleTrackingEvent(event2);

        verify(eventTracker, times(2)).trackEvent(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        List<TrackingRecord> allValues = captor.getAllValues();
        assertEventTracked(allValues.get(0), "url1", event1.getTimestamp());
        assertEventTracked(allValues.get(1), "url2", event2.getTimestamp());
    }

    @Test
    public void shouldTrackLikeEventsWithV1() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen("invoker_screen").build();
        UIEvent event = UIEvent.fromToggleLike(true,
                                               Urn.forTrack(123),
                                               eventContext,
                                               promotedSourceInfo,
                                               EntityMetadata.EMPTY);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForLikeEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForLikeEvent");
    }

    @Test
    public void shouldTrackUnlikeEventsWithV1() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen("invoker_screen").build();
        UIEvent event = UIEvent.fromToggleLike(false,
                                               Urn.forTrack(123),
                                               eventContext,
                                               promotedSourceInfo,
                                               EntityMetadata.EMPTY);
        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForUnlikeEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnlikeEvent");
    }

    @Test
    public void shouldTrackRepostEventsWithV1() {
        PropertySet trackProperties = TestPropertySets.expectedPromotedTrack();
        PromotedListItem promotedTrack = PromotedTrackItem.from(trackProperties);
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleRepost(true,
                                                 Urn.forTrack(123),
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 EntityMetadata.from(trackProperties));
        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForRepostEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForRepostEvent");
    }

    @Test
    public void shouldTrackUnRepostEventsV1() {
        PropertySet trackProperties = TestPropertySets.expectedPromotedTrack();
        PromotedListItem promotedTrack = PromotedTrackItem.from(trackProperties);
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleRepost(false,
                                                 Urn.forTrack(123),
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 EntityMetadata.from(trackProperties));
        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForUnRepostEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnRepostEvent");
    }

    @Test
    public void shouldTrackVisualAdCompanionImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        VisualAdImpressionEvent event = new VisualAdImpressionEvent(AdFixtures.getAudioAd(Urn.forTrack(123L)),
                                                                    Urn.forTrack(123L),
                                                                    Urn.forUser(456L),
                                                                    sourceInfo);
        when(dataBuilderv0.build(event)).thenReturn("ForVisualAdImpression");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForVisualAdImpression");
    }

    @Test
    public void shouldTrackLeaveBehindImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(AdFixtures.getLeaveBehindAd(Urn.forTrack(
                123L)), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(dataBuilderv0.build(event)).thenReturn("ForAudioAdImpression");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForAudioAdImpression");
    }

    @Test
    public void shouldTrackLeaveBehindClickTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(AdFixtures.getLeaveBehindAd(Urn.forTrack(
                123L)), Urn.forTrack(123), Urn.forUser(456), sourceInfo);
        when(dataBuilderv0.build(event)).thenReturn("ForAudioAdClick");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForAudioAdClick");
    }

    @Test
    public void shouldTrackPromotedTrackEvents() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent event = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");
        when(dataBuilderv0.build(event)).thenReturn("ForPromotedEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForPromotedEvent");
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
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForScreenEvent");
    }

    @Test
    public void shouldTrackUpsellEvent() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSettingsClick();
        when(dataBuilderv1.buildForUpsell(event)).thenReturn("ForUpsellEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUpsellEvent");
    }

    @Test
    public void shouldTrackCollectionEvent() {
        CollectionEvent event = CollectionEvent.forClearFilter();
        when(dataBuilderv1.buildForCollectionEvent(event)).thenReturn("ForCollectionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForCollectionEvent");
    }

    @Test
    public void shouldTrackAdPlaybackQuartileEvents() {
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forFirstQuartile(AdFixtures.getAudioAd(Urn.forTrack(321L)),
                                                                                 trackSourceInfo);
        when(dataBuilderv1.buildForAdProgressQuartileEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdPlayImpressionEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forPlay(videoAd, trackSourceInfo);
        when(dataBuilderv1.buildForAdImpression(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldNotTrackAdResumeEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        videoAd.setStartReported();
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forPlay(videoAd, trackSourceInfo);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker, never()).trackEvent(any(TrackingRecord.class));
    }

    @Test
    public void shouldNotTrackAdPauseEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                        trackSourceInfo,
                                                                        PlaybackSessionEvent.STOP_REASON_PAUSE);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker, never()).trackEvent(any(TrackingRecord.class));
    }

    @Test
    public void shouldTrackAdFinishEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                        trackSourceInfo,
                                                                        PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        when(dataBuilderv1.buildForAdFinished(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldVideoAdSkipEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromSkipVideoAdClick(videoAd, trackSourceInfo);
        when(dataBuilderv1.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldVideoAdClickthroughEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromVideoAdClickThrough(videoAd, trackSourceInfo);
        when(dataBuilderv1.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldTrackAdDeliveryEvents() {
        AdsReceived adsReceived = new AdsReceived(Urn.NOT_SET, Urn.NOT_SET, Urn.NOT_SET);
        AdDeliveryEvent event = AdDeliveryEvent.adDelivered(Urn.forTrack(123),
                                                            Urn.NOT_SET,
                                                            "endpoint",
                                                            adsReceived,
                                                            false,
                                                            false,
                                                            false);
        when(dataBuilderv1.buildForAdDelivery(event)).thenReturn("AdDeliveredEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdDeliveredEvent");
    }

    @Test
    public void shouldTrackAdFetchFailedEvents() {
        AdDeliveryEvent event = AdDeliveryEvent.adsRequestFailed(Urn.forTrack(123), "endpoint", false, false);
        when(dataBuilderv1.buildForAdDelivery(event)).thenReturn("AdFetchFailedEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdFetchFailedEvent");
    }

    @Test
    public void shouldTrackSearchSuggestionSearchEvents() {
        SearchEvent event = SearchEvent.searchSuggestion(Urn.forTrack(1), false, searchQuerySourceInfo);
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
        OfflineInteractionEvent event = OfflineInteractionEvent.fromEnableOfflineLikes("page_name");
        assertThat(v1OfflineInteractionEventCaptor("ForOfflineLikesEvent", event)).isEqualTo("ForOfflineLikesEvent");
    }

    @Test
    public void shouldTrackCollectionToOfflineEvent() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromEnableCollectionSync("page_name");
        assertThat(v1OfflineInteractionEventCaptor("ForOfflineCollection", event)).isEqualTo("ForOfflineCollection");
    }

    @Test
    public void shouldTrackPlaylistToOfflineEvent() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromAddOfflinePlaylist("page_name",
                                                                                       Urn.forPlaylist(123L),
                                                                                       null);
        assertThat(v1OfflineInteractionEventCaptor("ForOfflinePlaylistEvent", event)).isEqualTo(
                "ForOfflinePlaylistEvent");
    }

    @Test
    public void shouldTrackOfflineSyncStartEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromStarted(trackUrn, mock(TrackingMetadata.class));
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackOfflineSyncCompleteEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCompleted(trackUrn, mock(TrackingMetadata.class));
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackOfflineSyncFailEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromFailed(trackUrn, mock(TrackingMetadata.class));
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackOfflineSyncCancelEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCancelled(trackUrn, mock(TrackingMetadata.class));
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldForwardFlushCallToEventTracker() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventTracker).flush(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
    }

    private String v1OfflinePerformanceEventCaptor(String name, OfflinePerformanceEvent event) {
        when(dataBuilderv1.buildForOfflinePerformanceEvent(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private String v1OfflineInteractionEventCaptor(String name, OfflineInteractionEvent event) {
        when(dataBuilderv1.buildForOfflineInteractionEvent(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private String searchEventUrlCaptor(String name, SearchEvent event) {
        when(dataBuilderv0.build(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTracker).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private EventContextMetadata.Builder eventContextBuilder() {
        return EventContextMetadata.builder()
                                   .contextScreen("context_screen")
                                   .pageName("page_name")
                                   .pageUrn(Urn.NOT_SET);
    }

    private void assertEventTracked(TrackingRecord record, String data, long ts) {
        assertThat(record.getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(record.getTimeStamp()).isEqualTo(ts);
        assertThat(record.getData()).isEqualTo(data);
    }
}
