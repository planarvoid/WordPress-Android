package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.InterstitialAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class EventLoggerJsonDataBuilderTest extends AndroidUnitTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final long TIMESTAMP = new Date().getTime();
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";
    private static final String CDN_URL = "host.com";
    private static final String SCREEN_TAG = "screen_tag";
    private static final String CONTEXT_TAG = "context_tag";
    private static final String PAGE_NAME = "page_name";

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;

    private EventLoggerJsonDataBuilder jsonDataBuilder;
    private final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.LIKES.get(), true);
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:click:urn"));

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerJsonDataBuilder(context().getResources(), experimentOperations,
                deviceHelper, accountOperations, jsonTransformer);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUdid()).thenReturn(UDID);
    }

    @Test
    public void createsScreenEventJson() throws ApiMapperException {
        ScreenEvent screenEvent = ScreenEvent.create(Screen.ACTIVITIES);

        jsonDataBuilder.build(screenEvent);

        verify(jsonTransformer).toJson(eq(getEventData("pageview", "v0.0.0", screenEvent.getTimestamp()).pageName(Screen.ACTIVITIES.get())));
    }

    @Test
    public void createsAudioAdCompanionDisplayClickEventJson() throws ApiMapperException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final AudioAd audioAd = AdFixtures.getAudioAd(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);

        jsonDataBuilder.build(UIEvent.fromAudioAdCompanionDisplayClick(audioAd, audioAdTrackUrn, LOGGED_IN_USER, trackSourceInfo, TIMESTAMP));

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", TIMESTAMP)
                .adUrn(audioAd.getVisualAd().getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .clickName("clickthrough::companion_display")
                .clickTarget(audioAd.getVisualAd().getClickThroughUrl().toString())
                .clickObject(audioAdTrackUrn.toString())
                .externalMedia(audioAd.getVisualAd().getImageUrl().toString())
                .monetizedObject(monetizedTrackUrn.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsAudioAdSkippedClickEventJson() throws ApiMapperException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final AudioAd audioAd = AdFixtures.getAudioAd(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);

        jsonDataBuilder.build(UIEvent.fromSkipAudioAdClick(audioAd, audioAdTrackUrn, LOGGED_IN_USER, trackSourceInfo, TIMESTAMP));

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", TIMESTAMP)
                .adUrn(audioAd.getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .clickName("ad::skip")
                .clickObject(audioAdTrackUrn.toString())
                .externalMedia(audioAd.getVisualAd().getImageUrl().toString())
                .monetizedObject(monetizedTrackUrn.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsJsonForLeaveBehindImpression() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));

        jsonDataBuilder.build(AdOverlayTrackingEvent.forImpression(TIMESTAMP, leaveBehindAd, monetizedTrack, LOGGED_IN_USER, trackSourceInfo));

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", TIMESTAMP)
                .adUrn(leaveBehindAd.getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .impressionName("leave_behind")
                .impressionObject(leaveBehindAd.getAudioAdUrn().toString())
                .externalMedia(leaveBehindAd.getImageUrl())
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsJsonForInterstitialImpression() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final InterstitialAd interstitialAd = AdFixtures.getInterstitialAd(Urn.forTrack(123L));
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TIMESTAMP, interstitialAd, monetizedTrack, LOGGED_IN_USER, trackSourceInfo);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", TIMESTAMP)
                .adUrn(interstitialAd.getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .impressionName("interstitial")
                .impressionObject(monetizedTrack.toString())
                .externalMedia(interstitialAd.getImageUrl())
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("interstitial"));
    }

    @Test
    public void createsJsonForLeaveBehindClick() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(TIMESTAMP, leaveBehindAd, monetizedTrack, LOGGED_IN_USER, trackSourceInfo);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", TIMESTAMP)
                .adUrn(leaveBehindAd.getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .clickName("clickthrough::leave_behind")
                .clickObject(leaveBehindAd.getAudioAdUrn().toString())
                .clickTarget(leaveBehindAd.getClickthroughUrl().toString())
                .externalMedia(leaveBehindAd.getImageUrl())
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsJsonForInterstitialClick() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final InterstitialAd interstitialAd = AdFixtures.getInterstitialAd(Urn.forTrack(123L));
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(TIMESTAMP, interstitialAd, monetizedTrack, LOGGED_IN_USER, trackSourceInfo);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", TIMESTAMP)
                .adUrn(interstitialAd.getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .clickName("clickthrough::interstitial")
                .clickTarget(interstitialAd.getClickthroughUrl().toString())
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("interstitial")
                .externalMedia(interstitialAd.getImageUrl()));
    }

    @Test
    public void createsImpressionJsonForCompanionDisplayToAudioAd() throws ApiMapperException {
        Urn audioAdTrackUrn = Urn.forTrack(123L);
        final AudioAd audioAd = AdFixtures.getAudioAd(audioAdTrackUrn);

        jsonDataBuilder.build(new VisualAdImpressionEvent(audioAd, audioAdTrackUrn, LOGGED_IN_USER, trackSourceInfo, TIMESTAMP));

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", TIMESTAMP)
                .adUrn(audioAd.getVisualAd().getAdUrn().toString())
                .pageName(Screen.LIKES.get())
                .impressionName("companion_display")
                .impressionObject(audioAdTrackUrn.toString())
                .monetizationType("audio_ad")
                .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                .externalMedia(audioAd.getVisualAd().getImageUrl().toString()));
    }

    @Test
    public void createsAudioAdFinishedEventJson() throws ApiMapperException, CreateModelException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final AudioAd audioAd = AdFixtures.getAudioAd(monetizedTrackUrn);
        final PlaybackSessionEvent stopEvent = TestEvents.playbackSessionStopEvent();

        jsonDataBuilder.buildForAdFinished(stopEvent.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", stopEvent.getTimestamp())
                .pageName(stopEvent.getTrackSourceInfo().getOriginScreen())
                .adUrn(audioAd.getAdUrn().toString())
                .clickName("ad::finish")
                .clickObject(stopEvent.getTrackUrn().toString())
                .monetizedObject(monetizedTrackUrn.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsImpressionJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);

        jsonDataBuilder.buildForAudioAdImpression(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .adUrn(audioAd.getAdUrn().toString())
                .impressionName("audio_ad_impression")
                .impressionObject(audioAdTrack.get(TrackProperty.URN).toString())
                .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsJsonForLikeEvent() throws ApiMapperException {
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).build();
        final UIEvent event = UIEvent.fromToggleLike(true, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::add")
                .clickObject(Urn.forTrack(123).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForLikeEventWithPageUrn() throws ApiMapperException {
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).pageUrn(Urn.forPlaylist(321)).build();
        final UIEvent event = UIEvent.fromToggleLike(true, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::add")
                .clickObject(Urn.forTrack(123).toString())
                .pageUrn(Urn.forPlaylist(321).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPromotedItemLikeEvent() throws ApiMapperException {
        PromotedListItem item = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).build();
        final UIEvent event = UIEvent.fromToggleLike(true, Urn.forTrack(123), eventContext, promotedSourceInfo, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::add")
                .clickObject(Urn.forTrack(123).toString())
                .adUrn(item.getAdUrn())
                .monetizationType("promoted")
                .promotedBy(item.getPromoterUrn().get().toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForUnlikeEvent() throws ApiMapperException {
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).build();
        final UIEvent event = UIEvent.fromToggleLike(false, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::remove")
                .clickObject(Urn.forTrack(123).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForUnlikeEventWithPageUrn() throws ApiMapperException {
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).pageUrn(Urn.forPlaylist(321)).build();
        final UIEvent event = UIEvent.fromToggleLike(false, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::remove")
                .clickObject(Urn.forTrack(123).toString())
                .pageUrn(Urn.forPlaylist(321).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPromotedItemUnlikeEvent() throws ApiMapperException {
        PromotedListItem item = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).build();
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final UIEvent event = UIEvent.fromToggleLike(false, Urn.forTrack(123), eventContext, promotedSourceInfo, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::remove")
                .clickObject(Urn.forTrack(123).toString())
                .adUrn(item.getAdUrn())
                .monetizationType("promoted")
                .promotedBy(item.getPromoterUrn().get().toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForRepostEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleRepost(true, Urn.forTrack(123), eventContextBuilder().build(), null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("repost::add")
                .clickObject(Urn.forTrack(123).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForRepostEventWithPageUrn() throws ApiMapperException {
        final EventContextMetadata eventContext = eventContextBuilder().pageUrn(Urn.forPlaylist(321)).build();
        final UIEvent event = UIEvent.fromToggleRepost(true, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("repost::add")
                .clickObject(Urn.forTrack(123).toString())
                .pageUrn(Urn.forPlaylist(321).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPromotedItemRepostEvent() throws ApiMapperException {
        PropertySet trackProperties = TestPropertySets.expectedPromotedTrack();
        PromotedListItem item = PromotedTrackItem.from(trackProperties);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final EventContextMetadata eventContext = eventContextBuilder().build();
        final UIEvent event = UIEvent.fromToggleRepost(true, Urn.forTrack(123), eventContext, promotedSourceInfo, EntityMetadata.from(trackProperties));

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("repost::add")
                .clickObject(Urn.forTrack(123).toString())
                .adUrn(item.getAdUrn())
                .monetizationType("promoted")
                .promotedBy(item.getPromoterUrn().get().toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForUnRepostEvent() throws ApiMapperException {
        final EventContextMetadata eventContext = eventContextBuilder().build();
        final UIEvent event = UIEvent.fromToggleRepost(false, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("repost::remove")
                .clickObject(Urn.forTrack(123).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForUnRepostEventWithPageUrn() throws ApiMapperException {
        final EventContextMetadata eventContext = eventContextBuilder().pageUrn(Urn.forPlaylist(321)).build();
        final UIEvent event = UIEvent.fromToggleRepost(false, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("repost::remove")
                .clickObject(Urn.forTrack(123).toString())
                .pageUrn(Urn.forPlaylist(321).toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPromotedItemUnRepostEvent() throws ApiMapperException {
        PropertySet trackProperties = TestPropertySets.expectedPromotedTrack();
        PromotedListItem item = PromotedTrackItem.from(trackProperties);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final EventContextMetadata eventContext = eventContextBuilder().build();
        final UIEvent event = UIEvent.fromToggleRepost(false, Urn.forTrack(123), eventContext, promotedSourceInfo, EntityMetadata.from(trackProperties));

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("repost::remove")
                .clickObject(Urn.forTrack(123).toString())
                .adUrn(item.getAdUrn())
                .monetizationType("promoted")
                .promotedBy(item.getPromoterUrn().get().toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("play")
                .source("source")
                .sourceVersion("source-version")
                .playlistId("123")
                .playlistPositionV0("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g"));
    }

    @Test
    public void createsAudioFinishedEventJson() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_CONCURRENT_STREAMING, false);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("stop")
                .source("source")
                .sourceVersion("source-version")
                .playlistId("123")
                .playlistPositionV0("2")
                .protocol("hls")
                .playerType("PLAYA")
                .reason("concurrent_streaming")
                .connectionType("3g"));
    }

    @Test
    public void createsAudioEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(audioAdTrack.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + audioAdTrack.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("play")
                .source("source")
                .sourceVersion("source-version")
                .playlistId("123")
                .playlistPositionV0("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn(audioAd.getAdUrn().toString())
                .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsStopAudioEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent playbackSessionEvent = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, playbackSessionEvent, 0L, 456L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));
        trackSourceInfo.setSearchQuerySourceInfo(searchQuerySourceInfo);

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(audioAdTrack.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + audioAdTrack.get(TrackProperty.URN).getNumericId())
                .action("stop")
                .reason("buffer_underrun")
                .trigger("manual")
                .source("source")
                .sourceVersion("source-version")
                .playlistId("123")
                .playlistPositionV0("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn(audioAd.getAdUrn().toString())
                .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                .monetizationType("audio_ad")
                .queryUrn("some:search:urn")
                .queryPosition(5));
    }

    @Test
    public void createAudioEventJsonWithAdMetadataForPromotedTrackPlay() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PromotedSourceInfo promotedSource = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.of(Urn.forUser(123L)), Arrays.asList("promoted1", "promoted2"));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withPromotedTrack(promotedSource));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("play")
                .source("source")
                .sourceVersion("source-version")
                .playlistId("123")
                .playlistPositionV0("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn("ad:urn:123")
                .monetizationType("promoted")
                .promotedBy("soundcloud:users:123"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "play"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "buffer"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "playlist"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForSeekEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "seek"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "fragmentRate"));
    }

    @Test
    public void createsPlaybackErrorJsonForErrorEvent() throws Exception {
        final String userAgent = "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)";
        when(deviceHelper.getUserAgent()).thenReturn(userAgent);
        PlaybackErrorEvent event = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3, ConnectionType.FOUR_G);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("audio_error", "v0.0.0", event.getTimestamp())
                .protocol("https")
                .connectionType("4g")
                .os(userAgent)
                .bitrate("128")
                .format("mp3")
                .errorCode("category")
                .url("cdn-uri"));
    }

    @Test
    public void createsClickSearchEventJsonForForResults() throws Exception {
        SearchEvent searchEvent = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        jsonDataBuilder.build(searchEvent);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", searchEvent.getTimestamp())
                .clickName("search")
                .queryUrn("some:search:urn"));
    }

    @Test
    public void createsClickSearchEventJsonForTapOnUser() throws Exception {
        SearchEvent searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        jsonDataBuilder.build(searchEvent);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", searchEvent.getTimestamp())
                .pageName("search:everything")
                .clickName("open_profile")
                .clickObject("some:click:urn")
                .queryUrn("some:search:urn")
                .queryPosition(5));
    }

    @Test
    public void createsClickSearchEventJsonForTapOnTrack() throws Exception {
        SearchEvent searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        jsonDataBuilder.build(searchEvent);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", searchEvent.getTimestamp())
                .pageName("search:everything")
                .clickName("play")
                .clickObject("some:click:urn")
                .queryUrn("some:search:urn")
                .queryPosition(5));
    }

    @Test
    public void createsClickSearchEventJsonForTapOnPlaylist() throws Exception {
        SearchEvent searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        jsonDataBuilder.build(searchEvent);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", searchEvent.getTimestamp())
                .pageName("search:everything")
                .clickName("open_playlist")
                .clickObject("some:click:urn")
                .queryUrn("some:search:urn")
                .queryPosition(5));
    }

    @Test
    public void createsClickSearchEventJsonForTapOnSuggestion() throws Exception {
        SearchEvent searchEvent = SearchEvent.searchSuggestion(Urn.forTrack(1), false, searchQuerySourceInfo);
        jsonDataBuilder.build(searchEvent);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", searchEvent.getTimestamp())
                .pageName("search:suggestions")
                .clickName("item_navigation")
                .clickObject("some:click:urn")
                .queryUrn("some:search:urn")
                .queryPosition(5));
    }

    @Test
    public void createsStationsAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setStationSourceInfo(stationUrn, StationsSourceInfo.create(new Urn("soundcloud:radio:123-456")));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("play")
                .source("source")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .sourceUrn(stationUrn.toString())
                .queryUrn("soundcloud:radio:123-456")
                .connectionType("3g"));
    }

    @Test
    public void createsStationsSeedTrackAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setStationSourceInfo(stationUrn, StationsSourceInfo.create(Urn.NOT_SET));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("play")
                .source("source")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .sourceUrn(stationUrn.toString())
                .connectionType("3g"));
    }

    @Test
    public void createsStationsAudioEventJsonForAudioPauseEvent() throws ApiMapperException {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setStationSourceInfo(stationUrn, StationsSourceInfo.create(new Urn("soundcloud:radio:123-456")));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("stop")
                .source("source")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .sourceUrn(stationUrn.toString())
                .queryUrn("soundcloud:radio:123-456")
                .reason("playback_error")
                .connectionType("3g"));
    }

    @Test
    public void createsStationsSeedTrackAudioEventJsonForAudioPauseEvent() throws ApiMapperException {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setStationSourceInfo(stationUrn, StationsSourceInfo.create(Urn.NOT_SET));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(track.get(PlayableProperty.PLAY_DURATION))
                .sound("soundcloud:sounds:" + track.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .action("stop")
                .source("source")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .sourceUrn(stationUrn.toString())
                .reason("playback_error")
                .connectionType("3g"));
    }

    @Test
    public void createsPromotedTrackClickJson() throws Exception {
        PromotedListItem item = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent click = PromotedTrackingEvent.forPromoterClick(item, "stream");

        jsonDataBuilder.build(click);

        String promotedBy = item.getPromoterUrn().get().toString();
        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", click.getTimestamp())
                .pageName("stream")
                .monetizationType("promoted")
                .adUrn(item.getAdUrn())
                .promotedBy(promotedBy)
                .clickObject(item.getEntityUrn().toString())
                .clickTarget(promotedBy)
                .clickName("item_navigation"));
    }

    @Test
    public void createsPromotedTrackImpressionJson() throws Exception {
        PromotedListItem item = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(item, "stream");

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                .pageName("stream")
                .monetizationType("promoted")
                .adUrn(item.getAdUrn())
                .promotedBy(item.getPromoterUrn().get().toString())
                .impressionName("promoted_track")
                .impressionObject(item.getEntityUrn().toString()));
    }

    @Test
    public void createsPromotedPlaylistImpressionJson() throws Exception {
        PromotedListItem item = PromotedTrackItem.from(TestPropertySets.expectedPromotedPlaylist());
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(item, "stream");

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                .pageName("stream")
                .monetizationType("promoted")
                .adUrn(item.getAdUrn())
                .promotedBy(item.getPromoterUrn().get().toString())
                .impressionName("promoted_playlist")
                .impressionObject(item.getEntityUrn().toString()));
    }

    @Test
    public void createsPlayerUpsellImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forPlayerImpression(Urn.forTrack(1));

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                .pageUrn(Urn.forTrack(1).toString())
                .impressionName("consumer_sub_ad")
                .impressionObject("soundcloud:tcode:1017"));
    }

    @Test
    public void createsPlayerUpsellClickJson() throws Exception {
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forPlayerClick(Urn.forTrack(1));

        jsonDataBuilder.build(click);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", click.getTimestamp())
                .pageUrn(Urn.forTrack(1).toString())
                .clickName("clickthrough::consumer_sub_ad")
                .clickObject("soundcloud:tcode:1017"));
    }

    @Test
    public void createsUpsellImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forLikesImpression();

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                .impressionName("consumer_sub_ad")
                .impressionObject("soundcloud:tcode:1009"));
    }

    @Test
    public void createsUpsellClickJson() throws Exception {
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forPlaylistItemClick();

        jsonDataBuilder.build(click);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", click.getTimestamp())
                .clickName("clickthrough::consumer_sub_ad")
                .clickObject("soundcloud:tcode:1011"));
    }

    @Test
    public void createsUpgradeSuccessImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forUpgradeSuccess();

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                .impressionName("consumer_sub_upgrade_success"));
    }

    @Test
    public void addsCurrentExperimentJson() throws Exception {
        final EventContextMetadata eventContext = eventContextBuilder().invokerScreen(SCREEN_TAG).build();
        final UIEvent event = UIEvent.fromToggleLike(true, Urn.forTrack(123), eventContext, null, EntityMetadata.EMPTY);
        setupExperiments();

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", event.getTimestamp())
                .clickName("like::add")
                .clickObject(Urn.forTrack(123).toString())
                .experiment("exp_android_listening", 2345)
                .experiment("exp_android_ui", 3456)
                .pageName(PAGE_NAME));
    }

    private EventContextMetadata.Builder eventContextBuilder() {
        return EventContextMetadata.builder()
                .contextScreen(CONTEXT_TAG)
                .pageName(PAGE_NAME)
                .pageUrn(Urn.NOT_SET);
    }

    private EventLoggerEventData getPlaybackPerformanceEventFor(PlaybackPerformanceEvent event, String type) {
        return getEventData("audio_performance", "v0.0.0", event.getTimestamp())
                .latency(1000L)
                .protocol("https")
                .playerType("MediaPlayer")
                .connectionType("4g")
                .type(type)
                .host("host.com");
    }

    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, long timestamp) {
        return new EventLoggerEventData(eventName, boogalooVersion, 3152, UDID, LOGGED_IN_USER.toString(), timestamp);
    }

    private void setupExperiments() {
        HashMap<String, Integer> activeExperiments = new HashMap<>();
        activeExperiments.put("exp_android_listening", 2345);
        activeExperiments.put("exp_android_ui", 3456);
        when(experimentOperations.getTrackingParams()).thenReturn(activeExperiments);
    }

}
