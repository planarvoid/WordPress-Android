package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.events.UIEvent.PlayerInterface.FULLSCREEN;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_PAUSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.AdSessionEventArgs;
import com.soundcloud.android.events.AdsReceived;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.GoOnboardingTooltipEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.ScrollDepthEvent;
import com.soundcloud.android.events.SponsoredSessionStartEvent;
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
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;

public class EventLoggerAnalyticsProviderTest extends AndroidUnitTest {

    private EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;

    @Mock private EventTrackingManager eventTrackingManager;
    @Mock private EventLoggerV1JsonDataBuilder dataBuilder;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private FeatureFlags featureFlags;

    private Urn userUrn = Urn.forUser(123L);
    private Urn trackUrn = Urn.forTrack(123L);
    private TrackingMetadata trackingMetadata = new TrackingMetadata(userUrn, true, true);

    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() {
        eventLoggerAnalyticsProvider = new EventLoggerAnalyticsProvider(eventTrackingManager,
                                                                        InjectionSupport.lazyOf(dataBuilder),
                                                                        sharedPreferences, featureFlags);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();
        when(dataBuilder.buildForAudioEvent(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    @Test
    public void shouldTrackPlaybackPerformanceEventAsEventLoggerEvent() {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(PlaybackType.AUDIO_DEFAULT)
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HLS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost("uri")
                                                                 .format(PlaybackConstants.MediaType.UNKNOWN)
                                                                 .bitrate(0)
                                                                 .userUrn(userUrn)
                                                                 .build();
        when(dataBuilder.buildForPlaybackPerformance(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.timestamp());
    }

    @Test
    public void shouldTrackRichMediaPlaybackPerformanceEventAsEventLoggerEvent() {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(PlaybackType.VIDEO_AD)
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HLS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost("uri")
                                                                 .format("video/mp4")
                                                                 .bitrate(200)
                                                                 .userUrn(userUrn)
                                                                 .build();
        when(dataBuilder.buildForRichMediaPerformance(event)).thenReturn("url");

        eventLoggerAnalyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(dataBuilder, never()).buildForPlaybackPerformance(event);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(captor.getValue().getTimeStamp()).isEqualTo(event.timestamp());
        assertThat(captor.getValue().getData()).isEqualTo("url");
    }

    @Test
    public void shouldTrackPlaybackErrorEventAsEventLoggerEvent() {
        PlaybackErrorEvent event = new PlaybackErrorEvent("category",
                                                          PlaybackProtocol.HLS,
                                                          "uri",
                                                          PlaybackConstants.MediaType.MP3,
                                                          128000,
                                                          ConnectionType.FOUR_G,
                                                          PlayerType.FLIPPER);
        when(dataBuilder.buildForPlaybackError(event)).thenReturn("url");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handlePlaybackErrorEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertEventTracked(captor.getValue(), "url", event.getTimestamp());
    }

    @Test
    public void shouldTrackPlayerClickForward() {
        assertPlayerEvent(UIEvent.fromPlayerClickForward(FULLSCREEN));
    }

    @Test
    public void shouldTrackPlayerClickBackward() {
        assertPlayerEvent(UIEvent.fromPlayerClickBackward(FULLSCREEN));
    }

    @Test
    public void shouldTrackPlayerSwipeForward() {
        assertPlayerEvent(UIEvent.fromPlayerSwipeForward(FULLSCREEN));
    }

    @Test
    public void shouldTrackPlayerSwipeBackward() {
        assertPlayerEvent(UIEvent.fromPlayerSwipeBackward(FULLSCREEN));
    }

    @Test
    public void shouldTrackPlayerPlay() {
        assertPlayerEvent(UIEvent.fromPlayerPlay(FULLSCREEN));
    }

    @Test
    public void shouldTrackPlayerPause() {
        assertPlayerEvent(UIEvent.fromPlayerPause(FULLSCREEN));
    }

    private void assertPlayerEvent(UIEvent uiEvent) {
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        when(dataBuilder.buildForUIEvent(uiEvent)).thenReturn("player");

        eventLoggerAnalyticsProvider.handleTrackingEvent(uiEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("player");
    }

    @Test
    public void shouldTrackInteractionEventWhenIsInteractionEvent() {
        when(featureFlags.isEnabled(Flag.HOLISTIC_TRACKING)).thenReturn(true);

        final UIEvent event = UIEvent.fromPlayQueueShuffle(true);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        when(dataBuilder.isInteractionEvent(event)).thenReturn(true);
        when(dataBuilder.buildForInteractionEvent(event)).thenReturn("holistic tracking");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());
        final TrackingRecord record = captor.getAllValues().get(1);
        assertThat(record.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(record.getData()).isEqualTo("holistic tracking");
    }

    @Test
    public void shouldNotTrackInteractionEvent() {
        when(featureFlags.isEnabled(Flag.HOLISTIC_TRACKING)).thenReturn(true);
        final UIEvent event = UIEvent.fromPlayQueueShuffle(true);

        when(dataBuilder.isInteractionEvent(event)).thenReturn(false);
        when(dataBuilder.buildForInteractionEvent(event)).thenReturn("holistic tracking");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(any(TrackingRecord.class));
    }

    @Test
    public void shouldTrackLikeEventsWithV1() {
        TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleLike(true,
                                               Urn.forTrack(123),
                                               eventContext,
                                               promotedSourceInfo,
                                               EntityMetadata.EMPTY);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        when(dataBuilder.buildForUIEvent(event)).thenReturn("ForLikeEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForLikeEvent");
    }

    @Test
    public void shouldTrackUnlikeEventsWithV1() {
        TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleLike(false,
                                               Urn.forTrack(123),
                                               eventContext,
                                               promotedSourceInfo,
                                               EntityMetadata.EMPTY);
        when(dataBuilder.buildForUIEvent(event)).thenReturn("ForUnlikeEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnlikeEvent");
    }

    @Test
    public void shouldTrackFollowEventsWithV1() {
        final EntityMetadata userMetadata = EntityMetadata.fromUser(PlayableFixtures.user());
        EventContextMetadata eventContext = eventContextBuilder().build();

        UIEvent event = UIEvent.fromToggleFollow(true,
                                                 userMetadata,
                                                 eventContext);

        when(dataBuilder.buildForUIEvent(event)).thenReturn("ForFollowEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForFollowEvent");
    }

    @Test
    public void shouldTrackUnFollowEventsV1() {
        ApiUser userItem = PlayableFixtures.user();
        EventContextMetadata eventContext = eventContextBuilder().build();

        UIEvent event = UIEvent.fromToggleFollow(false,
                                                 EntityMetadata.fromUser(userItem),
                                                 eventContext);

        when(dataBuilder.buildForUIEvent(event)).thenReturn("ForUnFollowEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnFollowEvent");
    }

    @Test
    public void shouldTrackRepostEventsWithV1() {
        TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleRepost(true,
                                                 Urn.forTrack(123),
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 EntityMetadata.from(promotedTrack));
        when(dataBuilder.buildForUIEvent(event)).thenReturn("ForRepostEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForRepostEvent");
    }

    @Test
    public void shouldTrackUnRepostEventsV1() {
        TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleRepost(false,
                                                 Urn.forTrack(123),
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 EntityMetadata.from(promotedTrack));
        when(dataBuilder.buildForUIEvent(event)).thenReturn("ForUnRepostEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUnRepostEvent");
    }

    @Test
    public void shouldTrackVisualAdCompanionImpressionTrackingEvents() {
        TrackSourceInfo sourceInfo = new TrackSourceInfo("source", true);
        VisualAdImpressionEvent event = VisualAdImpressionEvent.create(AdFixtures.getAudioAd(Urn.forTrack(123L)),
                                                                    Urn.forUser(456L),
                                                                    sourceInfo);
        when(dataBuilder.buildForVisualAdImpression(event)).thenReturn("ForVisualAdImpression");
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
        when(dataBuilder.buildForAdOverlayTracking(event)).thenReturn("ForAudioAdImpression");
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
        when(dataBuilder.buildForAdOverlayTracking(event)).thenReturn("ForAudioAdClick");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForAudioAdClick");
    }

    @Test
    public void shouldTrackPromotedTrackEvents() {
        TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();
        PromotedTrackingEvent event = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");
        when(dataBuilder.buildForPromotedTracking(event)).thenReturn("ForPromotedEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForPromotedEvent");
    }

    @Test
    public void shouldNotTrackOtherUIEvents() {
        UIEvent event = UIEvent.fromStartStation();

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verifyZeroInteractions(eventTrackingManager);
    }

    @Test
    public void shouldTrackScreenEvent() {
        ScreenEvent event = ScreenEvent.create(Screen.ACTIVITIES);
        when(dataBuilder.buildForScreenEvent(event)).thenReturn("ForScreenEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForScreenEvent");
    }

    @Test
    public void shouldTrackUpsellEvent() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsImpression();
        when(dataBuilder.buildForUpsell(event)).thenReturn("ForUpsellEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForUpsellEvent");
    }

    @Test
    public void shouldTrackCollectionEvent() {
        CollectionEvent event = CollectionEvent.forClearFilter();
        when(dataBuilder.buildForCollectionEvent(event)).thenReturn("ForCollectionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForCollectionEvent");
    }

    @Test
    public void shouldTrackScrollDepthEvents() {
        final ScrollDepthEvent event = ScrollDepthEvent.create(Screen.STREAM,
                                                               ScrollDepthEvent.Action.START,
                                                               1,
                                                               Collections.emptyList(),
                                                               Collections.emptyList(),
                                                               Optional.absent());

        when(dataBuilder.buildForScrollDepthEvent(event)).thenReturn("ForScrollDepthEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("ForScrollDepthEvent");
    }

    @Test
    public void shouldTrackAdPlaybackErrorEvents() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdPlaybackErrorEvent event = AdPlaybackErrorEvent.failToBuffer(videoAd,
                                                                       TestPlayerTransitions.buffering(),
                                                                       videoAd.firstVideoSource());
        when(dataBuilder.buildForRichMediaErrorEvent(event)).thenReturn("AdPlaybackErrorEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackErrorEvent");
    }

    @Test
    public void shouldTrackAdPlaybackQuartileEvents() {
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forFirstQuartile(AdFixtures.getAudioAd(Urn.forTrack(321L)),
                                                                                 trackSourceInfo);
        when(dataBuilder.buildForAdPlaybackSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdPlaybackImpressionEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdSessionEventArgs adArgs = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.playing(), "123");
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forFinish(videoAd, adArgs);
        when(dataBuilder.buildForAdPlaybackSessionEvent(adEvent)).thenReturn("AdImpressionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(1);
        assertEventTracked(captor.getAllValues().get(0), "AdImpressionEvent", adEvent.getTimestamp());
    }

    @Test
    public void shouldTrackAdRichMediaEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdSessionEventArgs adArgs = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.playing(), "123");
        AdRichMediaSessionEvent adRichEvent = AdRichMediaSessionEvent.forPlay(videoAd, adArgs);
        when(dataBuilder.buildForRichMediaSessionEvent(adRichEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adRichEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(1);
        assertEventTracked(captor.getAllValues().get(0), "AdPlaybackSessionEvent", adRichEvent.getTimestamp());
    }

    @Test
    public void shouldTrackAdResumeEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        videoAd.setEventReported(PlayableAdData.ReportingEvent.START);
        AdSessionEventArgs adArgs = AdSessionEventArgs.create(trackSourceInfo,
                                                                              TestPlayerTransitions.playing(),
                                                                              "123");
        AdRichMediaSessionEvent adEvent = AdRichMediaSessionEvent.forPlay(videoAd, adArgs);

        when(dataBuilder.buildForRichMediaSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdPauseEvents() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdSessionEventArgs adArgs = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.idle(), "123");
        AdRichMediaSessionEvent adEvent = AdRichMediaSessionEvent.forStop(videoAd, adArgs, STOP_REASON_PAUSE);
        when(dataBuilder.buildForRichMediaSessionEvent(adEvent)).thenReturn("AdPlaybackSessionEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdPlaybackSessionEvent");
    }

    @Test
    public void shouldTrackAdFinishEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        AdSessionEventArgs adArgs = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.idle(), "123");
        AdPlaybackSessionEvent adEvent = AdPlaybackSessionEvent.forFinish(videoAd, adArgs);
        when(dataBuilder.buildForAdPlaybackSessionEvent(adEvent)).thenReturn("AdFinishedEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(1);
        assertEventTracked(captor.getAllValues().get(0), "AdFinishedEvent", adEvent.getTimestamp());
    }

    @Test
    public void shouldTackVideoMuteEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromVideoMute(videoAd, trackSourceInfo);
        when(dataBuilder.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldTackVideoUnmuteEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromVideoUnmute(videoAd, trackSourceInfo);
        when(dataBuilder.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldTrackVideoAdSkipEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromSkipAdClick(videoAd, trackSourceInfo);
        when(dataBuilder.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldTrackVideoAdClickthroughEvent() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent adEvent = UIEvent.fromPlayableClickThrough(videoAd, trackSourceInfo);
        when(dataBuilder.buildForUIEvent(adEvent)).thenReturn("UIEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(adEvent);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("UIEvent");
    }

    @Test
    public void shouldTrackAdDeliveryEvents() {
        AdDeliveryEvent event = AdDeliveryEvent.adDelivered(Optional.of(Urn.forTrack(123)),
                                                            Urn.forAd("dfp", "321"),
                                                            "abc-def-ghi",
                                                            false,
                                                            false);
        when(dataBuilder.buildForAdDelivery(event)).thenReturn("AdDeliveredEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdDeliveredEvent");
    }

    @Test
    public void shouldTrackAdRequestEvents() {
        AdsReceived adsReceived = AdsReceived.forPlayerAd(Urn.NOT_SET, Urn.NOT_SET, Urn.NOT_SET);
        AdRequestEvent event = AdRequestEvent.adRequestSuccess("abc-def-ghi",
                                                               Optional.of(Urn.forTrack(123)),
                                                               "endpoint",
                                                               adsReceived,
                                                               false,
                                                               false);
        when(dataBuilder.buildForAdRequest(event)).thenReturn("AdRequestEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("AdRequestEvent");
    }

    @Test
    public void shouldTrackStreamAdImpressionEvents() {
        final AppInstallAd appInstall = AdFixtures.getAppInstalls().get(0);
        final InlayAdImpressionEvent event = InlayAdImpressionEvent.create(appInstall, 42, 9876543210L);
        final ArgumentCaptor<TrackingRecord> eventCaptor = ArgumentCaptor.forClass(TrackingRecord.class);

        when(dataBuilder.buildForStreamAd(event)).thenReturn("StreamAdImpression");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData()).isEqualTo("StreamAdImpression");
    }

    @Test
    public void shouldTrackPrestitialAdImpressionEvents() {
        final PrestitialAdImpressionEvent event = PrestitialAdImpressionEvent.createForDisplay(AdFixtures.visualPrestitialAd());
        final ArgumentCaptor<TrackingRecord> eventCaptor = ArgumentCaptor.forClass(TrackingRecord.class);

        when(dataBuilder.buildForPrestitialAd(event)).thenReturn("PrestitialAdImpression");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData()).isEqualTo("PrestitialAdImpression");
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
        OfflineInteractionEvent event = OfflineInteractionEvent.fromAddOfflinePlaylist("page_name", Urn.forPlaylist(123L), null);
        assertThat(v1OfflineInteractionEventCaptor("ForOfflinePlaylistEvent", event)).isEqualTo("ForOfflinePlaylistEvent");
    }

    @Test
    public void shouldNotTrackSdCardAvailabilityEvent() {
        OfflineInteractionEvent event = OfflineInteractionEvent.forSdCardAvailable(true);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verifyZeroInteractions(eventTrackingManager);
    }

    @Test
    public void shouldTrackOfflineSyncStartEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromStarted(trackUrn, trackingMetadata);
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackOfflineSyncCompleteEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCompleted(trackUrn, trackingMetadata);
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackOfflineSyncFailEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromFailed(trackUrn, trackingMetadata);
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackOfflineSyncCancelEvent() {
        OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCancelled(trackUrn, trackingMetadata);
        assertThat(v1OfflinePerformanceEventCaptor("ForOfflineSyncEvent", event)).isEqualTo("ForOfflineSyncEvent");
    }

    @Test
    public void shouldTrackSponsoredSessionStartEvent() {
        SponsoredSessionStartEvent event = SponsoredSessionStartEvent.create(AdFixtures.sponsoredSessionAd(), Screen.PRESTITIAL);
        when(dataBuilder.buildForSponsoredSessionStartEvent(event)).thenReturn("SponsoredSessionStartEvent");
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("SponsoredSessionStartEvent");
    }

    @Test
    public void shouldTrackGoOnboardingTooltipEvent() {
        GoOnboardingTooltipEvent event = GoOnboardingTooltipEvent.forListenOfflineLikes();
        when(dataBuilder.buildForGoOnboardingTooltipEvent(event)).thenReturn("GoOnboardingTooltipEvent");

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager).trackEvent(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo("GoOnboardingTooltipEvent");
    }

    @Test
    public void shouldForwardFlushCallToEventTracker() {
        eventLoggerAnalyticsProvider.flush();
        verify(eventTrackingManager).flush(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
    }

    private String v1OfflinePerformanceEventCaptor(String name, OfflinePerformanceEvent event) {
        when(dataBuilder.buildForOfflinePerformanceEvent(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private String v1OfflineInteractionEventCaptor(String name, OfflineInteractionEvent event) {
        when(dataBuilder.buildForOfflineInteractionEvent(event)).thenReturn(name);
        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);

        eventLoggerAnalyticsProvider.handleTrackingEvent(event);

        verify(eventTrackingManager).trackEvent(captor.capture());
        return captor.getValue().getData();
    }

    private EventContextMetadata.Builder eventContextBuilder() {
        return EventContextMetadata.builder()
                                   .pageName("page_name")
                                   .pageUrn(Urn.NOT_SET);
    }

    private void assertEventTracked(TrackingRecord record, String data, long ts) {
        assertThat(record.getBackend()).isEqualTo(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(record.getTimeStamp()).isEqualTo(ts);
        assertThat(record.getData()).isEqualTo(data);
    }
}
