package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdPlaybackSessionEventArgs;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.AdRequestEvent.AdsReceived;
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
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
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

    @Mock private EventTrackingManager eventTrackingManager;
    @Mock private EventLoggerJsonDataBuilder dataBuilderv0;
    @Mock private EventLoggerV1JsonDataBuilder dataBuilderv1;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private FeatureFlags featureFlags;

    private Urn userUrn = Urn.forUser(123L);
    private Urn trackUrn = Urn.forTrack(123L);

    private TrackSourceInfo trackSourceInfo;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTrackingManager,
                                                                        InjectionSupport.lazyOf(dataBuilderv0),
                                                                        InjectionSupport.lazyOf(dataBuilderv1),
                                                                        sharedPreferences, featureFlags);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"),
                                                          5,
                                                          new Urn("some:clicked:urn"),
                                                          "query");
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(dataBuilderv1.buildForAudioEvent(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L,
                                                                             PlaybackProtocol.HLS,
                                                                             PlayerType.MEDIA_PLAYER,
                                                                             ConnectionType.FOUR_G,
                                                                             "uri",
                                                                             PlaybackConstants.MediaType.UNKNOWN,
                                                                             0,
                                                                             userUrn,
                                                                             PlaybackType.AUDIO_DEFAULT);
        when(dataBuilderv0.build(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
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
                                                                             PlaybackType.VIDEO_AD);
        when(dataBuilderv1.buildForRichMediaPerformance(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(dataBuilderv0, never()).build(event);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getValue().getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(captor.getValue().getData()).isEqualTo("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category",
                                                          PlaybackProtocol.HLS,
                                                          "uri",
                                                          PlaybackConstants.MediaType.MP3,
                                                          128000,
                                                          ConnectionType.FOUR_G);
        when(dataBuilderv0.build(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    @Test
    public void shouldSwipeSkipEvent() {
        assertSwipeEvent(UIEvent.fromSwipeSkip());
    }

    @Test
    public void shouldButtonSkipEvent() {
        assertSwipeEvent(UIEvent.fromButtonSkip());
    }

    @Test
    public void shouldSystemSkipEvent() {
        assertSwipeEvent(UIEvent.fromSystemSkip());
    }

    private void assertSwipeEvent(UIEvent uiEvent) {
        UIEvent event = uiEvent;
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForSkip");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForSkip");
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

        verify(eventTrackingManager).trackEvent(captor.capture());
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

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnlikeEvent");
    }

    @Test
    public void shouldTrackFollowEventsWithV1() {
        final EntityMetadata userMetadata = EntityMetadata.fromUser(TestPropertySets.user());
        EventContextMetadata eventContext = eventContextBuilder().build();

        UIEvent event = UIEvent.fromToggleFollow(true,
                                                 userMetadata,
                                                 eventContext);

        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForFollowEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForFollowEvent");
    }

    @Test
    public void shouldTrackUnFollowEventsV1() {
        PropertySet userProperties = TestPropertySets.user();
        EventContextMetadata eventContext = eventContextBuilder().build();

        UIEvent event = UIEvent.fromToggleFollow(false,
                                                 EntityMetadata.fromUser(userProperties),
                                                 eventContext);

        when(dataBuilderv1.buildForUIEvent(event)).thenReturn("ForUnFollowEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnFollowEvent");
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

        verify(eventTrackingManager).trackEvent(captor.capture());
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

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnRepostEvent");
    }

    @Test
    public void shouldTrackVisualAdCompanionImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        VisualAdImpressionEvent event = new VisualAdImpressionEvent(AdFixtures.getAudioAd(Urn.forTrack(123L)),
                                                                    Urn.forUser(456L),
                                                                    sourceInfo);
        when(dataBuilderv0.build(event)).thenReturn("ForVisualAdImpression");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
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

        verify(eventTrackingManager).trackEvent(captor.capture());
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

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForAudioAdClick");
    }

    @Test
    public void shouldTrackPromotedTrackEvents() {
        PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent event = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");
        when(dataBuilderv0.build(event)).thenReturn("ForPromotedEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForPromotedEvent");
    }

    @Test
    public void shouldNotTrackOtherUIEvents() {
        UIEvent event = new UIEvent(UIEvent.KIND_START_STATION);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verifyZeroInteractions(eventTrackingManager);
    }

    @Test
    public void shouldTrackScreenEvent() {
        ScreenEvent event = ScreenEvent.create(Screen.ACTIVITIES);
        when(dataBuilderv1.buildForScreenEvent(event)).thenReturn("ForScreenEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForScreenEvent");
    }

    @Test
    public void shouldTrackUpsellEvent() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSettingsClick();
        when(dataBuilderv1.buildForUpsell(event)).thenReturn("ForUpsellEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUpsellEvent");
    }

    @Test
    public void shouldTrackCollectionEvent() {
        CollectionEvent event = CollectionEvent.forClearFilter();
        when(dataBuilderv1.buildForCollectionEvent(event)).thenReturn("ForCollectionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForCollectionEvent");
    }

    @Test
    public void shouldTrackAdPlaybackErrorEvents() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackErrorEvent event = AdPlaybackErrorEvent.failToBuffer(videoAd,
                                                                       TestPlayerTransitions.buffering(),
                                                                       videoAd.getFirstSource());
        when(dataBuilderv1.buildForRichMediaErrorEvent(event)).thenReturn("AdPlaybackErrorEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackErrorEvent");
    }

    @Test
    public void shouldTrackAdPlaybackQuartileEvents() {
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forFirstQuartile(AdFixtures.getAudioAd(Urn.forTrack(321L)),
                                                                                 trackSourceInfo);
        when(dataBuilderv1.buildForAdProgressQuartileEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdPlaybackImpressionEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackSessionEventArgs adArgs = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                              TestPlayerTransitions.playing(),
                                                                              "123");
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forPlay(videoAd, adArgs);
        when(dataBuilderv1.buildForAdImpression(adEvent)).thenReturn("AdImpressionEvent");
        when(dataBuilderv1.buildForRichMediaSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);
        assertEventTracked(captor.getAllValues().get(0), "AdImpressionEvent", adEvent.getTimestamp());
        assertEventTracked(captor.getAllValues().get(1), "AdPlaybackSessionEvent", adEvent.getTimestamp());
    }

    @Test
    public void shouldTrackAdResumeEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        videoAd.setStartReported();
        AdPlaybackSessionEventArgs adArgs = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                              TestPlayerTransitions.playing(),
                                                                              "123");
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forPlay(videoAd, adArgs);

        when(dataBuilderv1.buildForRichMediaSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdPauseEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackSessionEventArgs adArgs = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                              TestPlayerTransitions.idle(),
                                                                              "123");
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                        adArgs,
                                                                        PlaybackSessionEvent.STOP_REASON_PAUSE);
        when(dataBuilderv1.buildForRichMediaSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdFinishEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackSessionEventArgs adArgs = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                              TestPlayerTransitions.idle(),
                                                                              "123");
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                        adArgs,
                                                                        PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        when(dataBuilderv1.buildForAdFinished(adEvent)).thenReturn("AdFinishedEvent");
        when(dataBuilderv1.buildForRichMediaSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);
        assertEventTracked(captor.getAllValues().get(0), "AdFinishedEvent", adEvent.getTimestamp());
        assertEventTracked(captor.getAllValues().get(1), "AdPlaybackSessionEvent", adEvent.getTimestamp());
    }

    @Test
    public void shouldVideoAdSkipEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromSkipAdClick(videoAd, trackSourceInfo);
        when(dataBuilderv1.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldVideoAdClickthroughEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromPlayerAdClickThrough(videoAd, trackSourceInfo);
        when(dataBuilderv1.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldTrackAdDeliveryEvents() {
        AdDeliveryEvent event = AdDeliveryEvent.adDelivered(Urn.forTrack(123),
                                                            Urn.forAd("dfp", "321"),
                                                            "abc-def-ghi",
                                                            false,
                                                            false);
        when(dataBuilderv1.buildForAdDelivery(event)).thenReturn("AdDeliveredEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdDeliveredEvent");
    }

    @Test
    public void shouldTrackAdRequestEvents() {
        AdsReceived adsReceived = new AdsReceived(Urn.NOT_SET, Urn.NOT_SET, Urn.NOT_SET);
        AdRequestEvent event = AdRequestEvent.adRequestSuccess("abc-def-ghi",
                                                               Urn.forTrack(123),
                                                               "endpoint",
                                                               adsReceived,
                                                               false,
                                                               false);
        when(dataBuilderv1.buildForAdRequest(event)).thenReturn("AdRequestEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdRequestEvent");
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
        verify(eventTrackingManager).flush(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
    }

    private String v1OfflinePerformanceEventCaptor(String name, OfflinePerformanceEvent event) {
        when(dataBuilderv1.buildForOfflinePerformanceEvent(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private String v1OfflineInteractionEventCaptor(String name, OfflineInteractionEvent event) {
        when(dataBuilderv1.buildForOfflineInteractionEvent(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private String searchEventUrlCaptor(String name, SearchEvent event) {
        when(dataBuilderv0.build(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
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
