package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ACTION_NAVIGATION;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_BUFFERING;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_CONCURRENT_STREAMING;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_ERROR;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
import static com.soundcloud.android.properties.Flag.HOLISTIC_TRACKING;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdPlaybackSessionEventArgs;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.AdsReceived;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.ScrollDepthEvent;
import com.soundcloud.android.events.ScrollDepthEvent.Action;
import com.soundcloud.android.events.ScrollDepthEvent.ItemDetails;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventLoggerV1JsonDataBuilderTest extends AndroidUnitTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final String CLIENT_EVENT_ID = "client-event-id";
    private static final String PLAY_ID = "play-id";
    private static final int APP_VERSION_CODE = 386;
    private static final int CLIENT_ID = 3152;
    private static final String BOOGALOO_VERSION = "v1.26.0";
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn CREATOR_URN = Urn.forUser(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Urn STATION_URN = Urn.forTrackStation(123L);
    private static final Plan CONSUMER_SUBS_PLAN = Plan.HIGH_TIER;
    private static final Urn QUERY_URN = new Urn("soundcloud:stations:6d2547a");
    private static final Urn CLICK_OBJECT_URN = new Urn("soundcloud:track:123");
    private static final Urn AD_URN = Urn.forAd("dfp", "123");
    private static final String PAGE_NAME = "page_name";
    private static final String SOURCE = "stations";
    private static final int QUERY_POSITION = 0;
    private static final String SEARCH_QUERY = "searchQuery";

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private FeatureOperations featureOperations;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private FeatureFlags featureFlags;

    private EventLoggerV1JsonDataBuilder jsonDataBuilder;
    private final TrackSourceInfo trackSourceInfo = createTrackSourceInfo();
    private EventContextMetadata eventContextMetadata = createEventContextMetadata();
    private final EntityMetadata entityMetadata = EntityMetadata.EMPTY;

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerV1JsonDataBuilder(context().getResources(),
                                                           deviceHelper,
                                                           connectionHelper,
                                                           accountOperations,
                                                           jsonTransformer,
                                                           featureOperations,
                                                           experimentOperations,
                                                           featureFlags);

        when(connectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.WIFI);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUdid()).thenReturn(UDID);
        when(deviceHelper.getAppVersionCode()).thenReturn(APP_VERSION_CODE);
        when(featureOperations.getCurrentPlan()).thenReturn(CONSUMER_SUBS_PLAN);
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                createArgs(track, trackSourceInfo, 12L, true));

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .reposter(Urn.forUser(456L))
                                               .localStoragePlayback(true)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("play")
                                               .playId(PLAY_ID)
                                               .playheadPosition(12L)
                                               .source("source")
                                               .sourceVersion("source-version")
                                               .inPlaylist(PLAYLIST_URN)
                                               .playlistPosition(2)
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackStartEvent() throws ApiMapperException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlayStart(
                createArgs(track, trackSourceInfo, 12L, true));

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .reposter(Urn.forUser(456L))
                                               .localStoragePlayback(true)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("play_start")
                                               .playheadPosition(12L)
                                               .source("source")
                                               .sourceVersion("source-version")
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .inPlaylist(PLAYLIST_URN)
                                               .playlistPosition(2)
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEventForStationsSeedTrack() throws ApiMapperException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                createArgs(track, trackSourceInfo, 12L, true));

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));
        trackSourceInfo.setStationSourceInfo(QUERY_URN, StationsSourceInfo.create(Urn.NOT_SET));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .reposter(Urn.forUser(456L))
                                               .localStoragePlayback(true)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("play")
                                               .playId(PLAY_ID)
                                               .playheadPosition(12L)
                                               .source("source")
                                               .sourceUrn(QUERY_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .sourceVersion("source-version")
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEventForStations() throws ApiMapperException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                createArgs(track, trackSourceInfo, 12L, true));

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));
        trackSourceInfo.setStationSourceInfo(QUERY_URN,
                                             StationsSourceInfo.create(new Urn("soundcloud:radio:123-456")));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .reposter(Urn.forUser(456L))
                                               .localStoragePlayback(true)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("play")
                                               .playId(PLAY_ID)
                                               .playheadPosition(12L)
                                               .source("source")
                                               .sourceUrn(QUERY_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .sourceVersion("source-version")
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioPauseEventJson() throws ApiMapperException, CreateModelException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(track, trackSourceInfo, 0L, false));
        final PlaybackSessionEventArgs args = createArgs(track, trackSourceInfo, 123L, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(playEvent,
                                                                        STOP_REASON_CONCURRENT_STREAMING,
                                                                        args);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .localStoragePlayback(false)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("pause")
                                               .playId(PLAY_ID)
                                               .playheadPosition(123L)
                                               .source("source")
                                               .sourceVersion("source-version")
                                               .inPlaylist(PLAYLIST_URN)
                                               .playlistPosition(2)
                                               .protocol("hls")
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL)
                                               .reason("concurrent_streaming"));
    }

    @Test
    public void createsAudioCheckpointEventJson() throws Exception {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forCheckpoint(
                createArgs(track, trackSourceInfo, 12L, true));

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .reposter(Urn.forUser(456L))
                                               .localStoragePlayback(true)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("checkpoint")
                                               .playId(PLAY_ID)
                                               .playheadPosition(12L)
                                               .source("source")
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .sourceVersion("source-version")
                                               .inPlaylist(PLAYLIST_URN)
                                               .playlistPosition(2)
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioPauseEventJsonForStationsForSeedTrack() throws ApiMapperException, CreateModelException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setStationSourceInfo(QUERY_URN, StationsSourceInfo.create(Urn.NOT_SET));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(track, trackSourceInfo, 0L, false));
        final PlaybackSessionEventArgs args = createArgs(track, trackSourceInfo, 123L, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(playEvent,
                                                                        STOP_REASON_ERROR,
                                                                        args);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .localStoragePlayback(false)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("pause")
                                               .playId(PLAY_ID)
                                               .playheadPosition(123L)
                                               .source("source")
                                               .sourceUrn(QUERY_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .sourceVersion("source-version")
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL)
                                               .reason("playback_error"));
    }

    @Test
    public void createsAudioPauseEventJsonForStations() throws ApiMapperException, CreateModelException {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setStationSourceInfo(QUERY_URN,
                                             StationsSourceInfo.create(QUERY_URN));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(track, trackSourceInfo, 0L, false));
        final PlaybackSessionEventArgs args = createArgs(track, trackSourceInfo, 123L, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(playEvent,
                                                                        STOP_REASON_ERROR,
                                                                        args);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .localStoragePlayback(false)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("pause")
                                               .playId(PLAY_ID)
                                               .playheadPosition(123L)
                                               .source("source")
                                               .sourceUrn(QUERY_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .sourceVersion("source-version")
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL)
                                               .reason("playback_error"));
    }

    @Test
    public void createAudioEventJsonWithAdMetadataForPromotedTrackPlay() throws Exception {
        final TrackItem track = PlayableFixtures.expectedTrackForPlayer();
        final PromotedSourceInfo promotedSource = new PromotedSourceInfo("ad:urn:123",
                                                                         Urn.forTrack(123L),
                                                                         Optional.of(Urn.forUser(123L)),
                                                                         Arrays.asList("promoted1", "promoted2"));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(createArgs(track, trackSourceInfo, 12L, false));

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(PlaybackSessionEvent.copyWithPromotedTrack(event, promotedSource));

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(event.trackSourceInfo().getOriginScreen())
                                               .trackLength(track.fullDuration())
                                               .track(track.getUrn())
                                               .trackOwner(track.creatorUrn())
                                               .localStoragePlayback(false)
                                               .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                                               .trigger("manual")
                                               .action("play")
                                               .playId(PLAY_ID)
                                               .playheadPosition(12L)
                                               .source("source")
                                               .sourceVersion("source-version")
                                               .inPlaylist(PLAYLIST_URN)
                                               .playlistPosition(2)
                                               .protocol("hls")
                                               .playerType("PLAYA")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .monetizationModel(PlayableFixtures.MONETIZATION_MODEL)
                                               .adUrn("ad:urn:123")
                                               .monetizationType("promoted")
                                               .promotedBy("soundcloud:users:123"));
    }

    @Test
    public void createsJsonForScrollDepthEvent() throws ApiMapperException {
        final ItemDetails details = ItemDetails.create(0, 1, 23.3123f);
        final List<ItemDetails> itemDetails = Collections.singletonList(details);
        final Optional<ReferringEvent> referringEvent = Optional.of(ReferringEvent.create("id", "kind"));
        final ScrollDepthEvent event = ScrollDepthEvent.create(Screen.STREAM, Action.SCROLL_START, 1,
                                                               itemDetails, itemDetails, referringEvent);

        jsonDataBuilder.buildForScrollDepthEvent(event);
        verify(jsonTransformer).toJson(getEventData("list_view_interaction", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName("stream:main")
                                               .action("scroll_start")
                                               .columnCount(1)
                                               .itemDetails("earliest_item", details)
                                               .itemDetails("latest_item", details)
                                               .clientEventId(event.id())
                                               .referringEvent(referringEvent));
    }

    @Test
    public void createsJsonForGoOnboardingStartEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromOnboardingStart();

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("offline_sync_onboarding::start")
                                               .clickCategory("consumer_subs"));
    }

    @Test
    public void createsJsonForGoOnboardingDismissEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromOnboardingDismiss();

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("offline_sync_onboarding::dismiss")
                                               .clickCategory("consumer_subs"));
    }

    @Test
    public void createsJsonForGoOnboardingAutomaticSyncEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromOnboardingWithAutomaticSync();

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("offline_sync_onboarding::automatic_collection_sync")
                                               .clickCategory("consumer_subs"));
    }

    @Test
    public void createsJsonForGoOnboardingManualSyncEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromOnboardingWithManualSync();

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("offline_sync_onboarding::manual_sync")
                                               .clickCategory("consumer_subs"));
    }

    @Test
    public void createsJsonForWifiOnlyOfflineSyncEnabledEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.forOnlyWifiOverWifiToggle(true);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("only_sync_over_wifi::enable")
                                               .clickCategory("consumer_subs"));
    }

    @Test
    public void createsJsonForWifiOnlyOfflineSyncDisabledEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.forOnlyWifiOverWifiToggle(false);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("only_sync_over_wifi::disable")
                                               .clickCategory("consumer_subs"));
    }

    @Test
    public void createsJsonForEnableOfflineCollectionSyncEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromEnableCollectionSync(PAGE_NAME);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("automatic_collection_sync::enable")
                                               .clickCategory("consumer_subs")
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForDisableOfflineCollectionEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromDisableCollectionSync(PAGE_NAME);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("automatic_collection_sync::disable")
                                               .clickCategory("consumer_subs")
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForDisableOfflineCollectionFromPlaylistEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromDisableCollectionSync(PAGE_NAME,
                                                                                                Optional.of(PLAYLIST_URN));

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("automatic_collection_sync::disable")
                                               .clickCategory("consumer_subs")
                                               .clickObject(PLAYLIST_URN.toString())
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineAddEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromAddOfflinePlaylist(PAGE_NAME,
                                                                                             PLAYLIST_URN,
                                                                                             null);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickCategory("consumer_subs")
                                               .clickName("playlist_to_offline::add")
                                               .clickObject(String.valueOf(PLAYLIST_URN))
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflinePlaylist(PAGE_NAME,
                                                                                                PLAYLIST_URN,
                                                                                                null);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickCategory("consumer_subs")
                                               .clickName("playlist_to_offline::remove")
                                               .clickObject(String.valueOf(PLAYLIST_URN))
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEventForPromotedItem() throws ApiMapperException {
        final PromotedListItem item = PlayableFixtures.expectedPromotedPlaylist();
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflinePlaylist(PAGE_NAME,
                                                                                                PLAYLIST_URN,
                                                                                                promotedSourceInfo);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickCategory("consumer_subs")
                                               .clickName("playlist_to_offline::remove")
                                               .clickObject(String.valueOf(PLAYLIST_URN))
                                               .pageName(PAGE_NAME)
                                               .adUrn(item.getAdUrn())
                                               .monetizationType("promoted")
                                               .promotedBy(item.getPromoterUrn().get().toString()));
    }

    @Test
    public void createsJsonForEnableOfflineLikesEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromEnableOfflineLikes(PAGE_NAME);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("automatic_likes_sync::enable")
                                               .clickCategory("consumer_subs")
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForLikesToOfflineRemoveEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflineLikes(PAGE_NAME);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("automatic_likes_sync::disable")
                                               .clickCategory("consumer_subs")
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForOfflineStorageLimitBelowUsage() throws Exception {
        OfflineInteractionEvent impression = OfflineInteractionEvent.forStorageBelowLimitImpression();

        jsonDataBuilder.buildForOfflineInteractionEvent(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                                               .pageName("settings:offline_sync_settings")
                                               .impressionCategory("consumer_subs")
                                               .impressionName("offline_storage::limit_below_usage"));
    }

    @Test
    public void createJsonFromOfflineSyncStartEventWithPlaylistTrackContext() throws ApiMapperException {
        final TrackingMetadata trackContext = new TrackingMetadata(CREATOR_URN, true, false);
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromStarted(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflinePerformanceEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", BOOGALOO_VERSION, event.getTimestamp())
                                               .track(TRACK_URN)
                                               .trackOwner(CREATOR_URN)
                                               .inOfflinePlaylist(false)
                                               .inOfflineLikes(true)
                                               .eventStage("start")
        );
    }

    @Test
    public void createJsonFromOfflineSyncFailEventWithLikeTrackContext() throws ApiMapperException {
        final TrackingMetadata trackContext = new TrackingMetadata(CREATOR_URN, true, false);
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromFailed(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflinePerformanceEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", BOOGALOO_VERSION, event.getTimestamp())
                                               .track(TRACK_URN)
                                               .trackOwner(CREATOR_URN)
                                               .inOfflinePlaylist(false)
                                               .inOfflineLikes(true)
                                               .eventStage("fail")
        );
    }

    @Test
    public void createJsonFromOfflineSyncCancelEventWithLikeTrackContext() throws ApiMapperException {
        final TrackingMetadata trackContext = new TrackingMetadata(CREATOR_URN, true, false);
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCancelled(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflinePerformanceEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", BOOGALOO_VERSION, event.getTimestamp())
                                               .track(TRACK_URN)
                                               .trackOwner(CREATOR_URN)
                                               .inOfflinePlaylist(false)
                                               .inOfflineLikes(true)
                                               .eventStage("user_cancelled")
        );
    }

    @Test
    public void createJsonFromOfflineSyncCompleteEventWithLikeTrackContext() throws ApiMapperException {
        final TrackingMetadata trackContext = new TrackingMetadata(CREATOR_URN, true, false);
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCompleted(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflinePerformanceEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", BOOGALOO_VERSION, event.getTimestamp())
                                               .track(TRACK_URN)
                                               .trackOwner(CREATOR_URN)
                                               .inOfflinePlaylist(false)
                                               .inOfflineLikes(true)
                                               .eventStage("complete")
        );
    }

    @Test
    public void createsJsonFromRichMediaErrorEvent() throws ApiMapperException {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackErrorEvent event = AdPlaybackErrorEvent.failToBuffer(videoAd,
                                                                             TestPlayerTransitions.buffering(),
                                                                             videoAd.getFirstSource());

        jsonDataBuilder.buildForRichMediaErrorEvent(event);

        verify(jsonTransformer).toJson(getEventData("rich_media_stream_error", BOOGALOO_VERSION, event.getTimestamp())
                                               .mediaType("video")
                                               .errorName("failToBuffer")
                                               .host("http://videourl.com/video.mp4")
                                               .format("mp4")
                                               .bitrate(1001)
                                               .playerType("player")
                                               .protocol("hls"));
    }

    @Test
    public void createsJsonFromRichMediaStreamPlayEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackSessionEventArgs eventArgs = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                                       TestPlayerTransitions.playing(),
                                                                                       CLIENT_EVENT_ID);
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        final AdRichMediaSessionEvent event = AdRichMediaSessionEvent.forPlay(audioAd, eventArgs);


        jsonDataBuilder.buildForRichMediaSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("rich_media_stream", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(eventArgs.getTrackSourceInfo().getOriginScreen())
                                               .trackLength(eventArgs.getDuration())
                                               .trigger("manual")
                                               .action("play")
                                               .playheadPosition(0L)
                                               .source("source")
                                               .sourceVersion("source-version")
                                               .inPlaylist(PLAYLIST_URN)
                                               .playlistPosition(2)
                                               .protocol("hls")
                                               .playerType("player")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .adUrn(audioAd.getAdUrn().toString())
                                               .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                                               .monetizationType("audio_ad"));
    }

    @Test
    public void createsJsonFromRichMediaStreamStopEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackSessionEventArgs eventArgs = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                                       TestPlayerTransitions.idle(),
                                                                                       CLIENT_EVENT_ID);
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        final AdRichMediaSessionEvent event = AdRichMediaSessionEvent.forStop(audioAd,
                                                                            eventArgs,
                                                                            STOP_REASON_BUFFERING);

        jsonDataBuilder.buildForRichMediaSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("rich_media_stream", BOOGALOO_VERSION, event.getTimestamp())
                                               .action("pause")
                                               .reason("buffer_underrun")
                                               .pageName(eventArgs.getTrackSourceInfo().getOriginScreen())
                                               .trackLength(eventArgs.getDuration())
                                               .trigger("manual")
                                               .playheadPosition(0L)
                                               .source("source")
                                               .sourceVersion("source-version")
                                               .inPlaylist(PLAYLIST_URN)
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .playlistPosition(2)
                                               .protocol("hls")
                                               .playerType("player")
                                               .clientEventId(CLIENT_EVENT_ID)
                                               .adUrn(audioAd.getAdUrn().toString())
                                               .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                                               .monetizationType("audio_ad"));
    }

    @Test
    public void createsJsonFromRichMediaPerformanceEvent() throws ApiMapperException {
        final PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(321,
                                                                                   PlaybackProtocol.HTTPS,
                                                                                   PlayerType.MEDIA_PLAYER,
                                                                                   ConnectionType.FOUR_G,
                                                                                   "host",
                                                                                   PlaybackConstants.MIME_TYPE_MP4,
                                                                                   1001,
                                                                                   Urn.NOT_SET,
                                                                                   PlaybackType.AUDIO_AD);

        jsonDataBuilder.buildForRichMediaPerformance(event);

        verify(jsonTransformer).toJson(getEventData("rich_media_stream_performance",
                                                    BOOGALOO_VERSION,
                                                    event.getTimestamp())
                                               .metric("timeToPlayMs", 321)
                                               .mediaType("audio")
                                               .protocol("https")
                                               .playerType("MediaPlayer")
                                               .host("host")
                                               .bitrate(1001)
                                               .format("mp4"));
    }

    @Test
    public void createsJsonFromAdDeliveryEvent() throws ApiMapperException {
        final AdDeliveryEvent event = AdDeliveryEvent.adDelivered(Optional.of(TRACK_URN),
                                                                  AD_URN,
                                                                  "abc-def-ghi",
                                                                  false,
                                                                  true);

        jsonDataBuilder.buildForAdDelivery(event);

        verify(jsonTransformer).toJson(getEventData("ad_delivery", BOOGALOO_VERSION, event.getTimestamp())
                                               .clientEventId(event.id())
                                               .adDelivered(AD_URN.toString())
                                               .monetizedObject(TRACK_URN.toString())
                                               .inForeground(true)
                                               .playerVisible(false)
                                               .adRequestId("abc-def-ghi"));
    }

    @Test
    public void createsJsonFromAdRequestSuccessEvent() throws ApiMapperException {
        final AdsReceived adsReceived = AdsReceived.forPlayerAd(AD_URN, Urn.NOT_SET, Urn.NOT_SET);
        final AdRequestEvent event = AdRequestEvent.adRequestSuccess("abc-def-ghi",
                                                                     Optional.of(TRACK_URN),
                                                                     "endpoint",
                                                                     adsReceived,
                                                                     true,
                                                                     false);
        when(jsonTransformer.toJson(adsReceived.ads)).thenReturn("{ads-received}");

        jsonDataBuilder.buildForAdRequest(event);

        verify(jsonTransformer).toJson(adsReceived.ads);
        verify(jsonTransformer).toJson(getEventData("ad_request", BOOGALOO_VERSION, event.getTimestamp())
                                               .clientEventId("abc-def-ghi")
                                               .adsRequestSuccess(true)
                                               .adsReceived("{ads-received}")
                                               .monetizedObject(TRACK_URN.toString())
                                               .playerVisible(true)
                                               .inForeground(false)
                                               .adsEndpoint("endpoint"));
    }

    @Test
    public void createsJsonFromAdRequestFailureEvent() throws ApiMapperException {
        final AdRequestEvent event = AdRequestEvent.adRequestFailure("abc-def-ghi",
                                                                     Optional.of(TRACK_URN),
                                                                     "endpoint",
                                                                     true,
                                                                     false);

        jsonDataBuilder.buildForAdRequest(event);

        verify(jsonTransformer).toJson(getEventData("ad_request", BOOGALOO_VERSION, event.getTimestamp())
                                               .clientEventId("abc-def-ghi")
                                               .adsRequestSuccess(false)
                                               .monetizedObject(TRACK_URN.toString())
                                               .inForeground(false)
                                               .playerVisible(true)
                                               .adsEndpoint("endpoint"));
    }

    @Test
    public void createsJsonForFirstQuartileAdPlaybackProgressEvent() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forFirstQuartile(AdFixtures.getVideoAd(TRACK_URN),
                                                                                     trackSourceInfo);

        jsonDataBuilder.buildForAdPlaybackSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::first_quartile"));
    }

    @Test
    public void createsJsonForSecondQuartileAdPlaybackProgressEvent() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forSecondQuartile(AdFixtures.getVideoAd(TRACK_URN),
                                                                                      trackSourceInfo);

        jsonDataBuilder.buildForAdPlaybackSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::second_quartile"));
    }

    @Test
    public void createsJsonForThirdQuartileAdPlaybackProgressEvent() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forThirdQuartile(AdFixtures.getVideoAd(TRACK_URN),
                                                                                     trackSourceInfo);

        jsonDataBuilder.buildForAdPlaybackSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::third_quartile"));
    }

    @Test
    public void createsJsonForFullScreenVideoAdUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromVideoAdFullscreen(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::full_screen"));
    }

    @Test
    public void createJsonForStreamAdImpressionEvent() throws ApiMapperException {
        final AppInstallAd appInstall = AdFixtures.getAppInstalls().get(0);
        final InlayAdImpressionEvent event = InlayAdImpressionEvent.create(appInstall, 42, 9876543210L);

        jsonDataBuilder.buildForStreamAd(event);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, 9876543210L)
                                               .monetizationType("mobile_inlay")
                                               .adUrn("dfp:ads:1")
                                               .pageName("stream:main")
                                               .impressionName("app_install")
                                               .contextPosition(42)
                                               .clientEventId(event.id()));
    }

    @Test
    public void createsJsonForSwipeSkipUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromSwipeSkip();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("swipe_skip")
                                               .clickCategory("player_interaction"));
    }

    @Test
    public void createsJsonForSystemSkipUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromSystemSkip();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("system_skip")
                                               .clickCategory("player_interaction"));
    }

    @Test
    public void createsJsonForButtonSkipUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromButtonSkip();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("button_skip")
                                               .clickCategory("player_interaction"));
    }

    @Test
    public void createsJsonForShrinkVideoAdUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromVideoAdShrink(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::exit_full_screen"));
    }

    @Test
    public void createsJsonForSkipVideoAdUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromSkipAdClick(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::skip"));
    }

    @Test
    public void createsJsonForVideoAdClickthroughUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayerAdClickThrough(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("clickthrough::video_ad")
                                               .clickTarget("http://clickthrough.videoad.com"));
    }

    @Test
    public void createsJsonForVideoAdImpression() throws ApiMapperException {
        final AdPlaybackSessionEventArgs args = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                                  TestPlayerTransitions.playing(),
                                                                                  "123");
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forPlay(AdFixtures.getVideoAd(TRACK_URN), args);

        jsonDataBuilder.buildForAdPlaybackSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .impressionName("video_ad_impression"));
    }

    @Test
    public void createsJsonForVideoAdFinish() throws ApiMapperException {
        final AdPlaybackSessionEventArgs args = AdPlaybackSessionEventArgs.create(trackSourceInfo,
                                                                                  TestPlayerTransitions.idle(),
                                                                                  "123");
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forStop(AdFixtures.getVideoAd(TRACK_URN),
                                                                            args,
                                                                            STOP_REASON_TRACK_FINISHED);

        jsonDataBuilder.buildForAdPlaybackSessionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .monetizedObject(TRACK_URN.toString())
                                               .adUrn("dfp:ads:905")
                                               .pageName("collection:likes")
                                               .monetizationType("video_ad")
                                               .clickName("ad::finish"));
    }

    @Test
    public void createsJsonForAppInstallClickthroughUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromAppInstallAdClickThrough(AppInstallAd.create(AdFixtures.getApiAppInstall()));

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .adUrn("dfp:ads:111")
                                               .monetizationType("mobile_inlay")
                                               .clickName("clickthrough::app_install")
                                               .clickTarget("http://clickthrough.com"));
    }

    @Test
    public void createdJsonForInteractionEventWithActiveAttributingActivity() throws ApiMapperException {
        final Integer position = 0;
        final Module module = Module.create(Module.STREAM, position);
        final Urn pageUrn = Urn.forUser(123L);
        final AttributingActivity attributingActivity = mock(AttributingActivity.class);
        when(attributingActivity.getType()).thenReturn(AttributingActivity.PROMOTED);
        when(attributingActivity.isActive(Optional.of(module))).thenReturn(true);

        final EventContextMetadata eventContextMetadata =
                EventContextMetadata.builder()
                                    .contextScreen("screen")
                                    .trackSourceInfo(trackSourceInfo)
                                    .pageName(PAGE_NAME)
                                    .module(module)
                                    .attributingActivity(attributingActivity)
                                    .linkType(LinkType.SELF)
                                    .pageUrn(pageUrn)
                                    .build();

        final String pageviewId = randomUUID().toString();

        final UIEvent navigationEvent = UIEvent.fromNavigation(TRACK_URN, eventContextMetadata).putReferringEvent(ReferringEvent.create(pageviewId, Strings.EMPTY));

        jsonDataBuilder.buildForUIEvent(navigationEvent);

        verify(jsonTransformer).toJson(getEventData("item_interaction",
                                                    BOOGALOO_VERSION,
                                                    navigationEvent.getTimestamp())
                                               .clientEventId(navigationEvent.id())
                                               .item(TRACK_URN.toString())
                                               .pageviewId(pageviewId)
                                               .pageName(PAGE_NAME)
                                               .action(ACTION_NAVIGATION)
                                               .linkType(LinkType.SELF.getName())
                                               .pageUrn(pageUrn.toString())
                                               .attributingActivity(attributingActivity.getType(),
                                                                    attributingActivity.getResource())
                                               .module(module.getName())
                                               .modulePosition(position)
                                               .queryUrn(navigationEvent.queryUrn().get().toString())
                                               .queryPosition(navigationEvent.queryPosition().get())
        );
    }

    @Test
    public void createdJsonForInteractionEventWithInactiveAttributingActivity() throws ApiMapperException {
        final Integer position = 0;
        final Module module = Module.create(Module.STREAM, position);
        final Urn pageUrn = Urn.forUser(123L);
        final AttributingActivity attributingActivity = mock(AttributingActivity.class);
        when(attributingActivity.getType()).thenReturn(AttributingActivity.PROMOTED);
        when(attributingActivity.isActive(Optional.of(module))).thenReturn(false);

        final EventContextMetadata eventContextMetadata =
                EventContextMetadata.builder()
                                    .contextScreen("screen")
                                    .trackSourceInfo(trackSourceInfo)
                                    .pageName(PAGE_NAME)
                                    .module(module)
                                    .attributingActivity(attributingActivity)
                                    .linkType(LinkType.SELF)
                                    .pageUrn(pageUrn)
                                    .build();

        final String pageviewId = randomUUID().toString();
        final UIEvent navigationEvent = UIEvent.fromNavigation(TRACK_URN, eventContextMetadata).putReferringEvent(ReferringEvent.create(pageviewId, Strings.EMPTY));

        jsonDataBuilder.buildForUIEvent(navigationEvent);

        verify(jsonTransformer).toJson(getEventData("item_interaction",
                                                    BOOGALOO_VERSION,
                                                    navigationEvent.getTimestamp())
                                               .clientEventId(navigationEvent.id())
                                               .item(TRACK_URN.toString())
                                               .pageviewId(pageviewId)
                                               .pageName(PAGE_NAME)
                                               .action(ACTION_NAVIGATION)
                                               .linkType(LinkType.SELF.getName())
                                               .pageUrn(pageUrn.toString())
                                               .module(module.getName())
                                               .modulePosition(position)
                                               .queryUrn(navigationEvent.queryUrn().get().toString())
                                               .queryPosition(navigationEvent.queryPosition().get())
        );
    }

    @Test
    public void createsInteractionEventJsonForFollowEvent() throws ApiMapperException {
        final EntityMetadata userMetadata = EntityMetadata.fromUser(PlayableFixtures.user());
        final Integer position = 0;
        final Module module = Module.create(Module.SINGLE, position);
        final EventContextMetadata eventContextMetadata =
                EventContextMetadata.builder()
                                    .contextScreen("screen")
                                    .pageName(PAGE_NAME)
                                    .module(module)
                                    .build();

        final String pageviewId = randomUUID().toString();
        final UIEvent event = UIEvent.fromToggleFollow(true, userMetadata, eventContextMetadata).putReferringEvent(ReferringEvent.create(pageviewId, Strings.EMPTY));

        jsonDataBuilder.buildForInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("item_interaction", BOOGALOO_VERSION, event.getTimestamp())
                                               .clientEventId(event.id())
                                               .item(userMetadata.creatorUrn.toString())
                                               .pageName(PAGE_NAME)
                                               .pageviewId(pageviewId)
                                               .action(UIEvent.Action.FOLLOW_ADD.key())
                                               .module(module.getName())
                                               .modulePosition(position)
        );
    }

    @Test
    public void createsClickEventJsonForFollowEvent() throws ApiMapperException {
        final EntityMetadata userMetadata = EntityMetadata.fromUser(PlayableFixtures.user());
        final Integer position = 0;
        final Module module = Module.create(Module.SINGLE, position);
        final EventContextMetadata eventContextMetadata =
                EventContextMetadata.builder()
                                    .contextScreen("screen")
                                    .pageName(PAGE_NAME)
                                    .module(module)
                                    .build();

        final UIEvent event = UIEvent.fromToggleFollow(true, userMetadata, eventContextMetadata);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName(UIEvent.Action.FOLLOW_ADD.key())
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(userMetadata.creatorUrn.toString())
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForShareEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromShareRequest(TRACK_URN, eventContextMetadata, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);
        assertEngagementClickEventJson("share::request", event.getTimestamp());
    }

    @Test
    public void addsCurrentExperimentJsonOmitsEmptyVariants() throws ApiMapperException {

        when(experimentOperations.getActiveVariants()).thenReturn(Lists.newArrayList(new Integer[]{}));

        final UIEvent event = UIEvent.fromShareRequest(TRACK_URN, eventContextMetadata, null, entityMetadata);
        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("share::request")
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(TRACK_URN.toString())
                                               .clickSource(SOURCE)
                                               .clickSourceUrn(STATION_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .pageName(PAGE_NAME)
                                               .pageUrn(TRACK_URN.toString())
        );
    }

    @Test
    public void addsCurrentExperimentJsonAddsSingleVariant() throws ApiMapperException {

        when(experimentOperations.getActiveVariants()).thenReturn(Lists.newArrayList(1234));

        final UIEvent event = UIEvent.fromShareRequest(TRACK_URN, eventContextMetadata, null, entityMetadata);
        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("share::request")
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(TRACK_URN.toString())
                                               .clickSource(SOURCE)
                                               .clickSourceUrn(STATION_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .pageName(PAGE_NAME)
                                               .experiment("part_of_variants", "1234")
                                               .pageUrn(TRACK_URN.toString())
        );
    }


    @Test
    public void addsCurrentExperimentJson() throws ApiMapperException {

        when(experimentOperations.getActiveVariants()).thenReturn(Lists.newArrayList(2345, 3456));

        final UIEvent event = UIEvent.fromShareRequest(TRACK_URN, eventContextMetadata, null, entityMetadata);
        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("share::request")
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(TRACK_URN.toString())
                                               .clickSource(SOURCE)
                                               .clickSourceUrn(STATION_URN.toString())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION)
                                               .pageName(PAGE_NAME)
                                               .experiment("part_of_variants", "2345,3456")
                                               .pageUrn(TRACK_URN.toString())
        );
    }

    @Test
    public void createsJsonForRepostEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleRepost(true, TRACK_URN, eventContextMetadata, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);
        assertEngagementClickEventJson("repost::add", event.getTimestamp());
    }

    @Test
    public void createsJsonForUnpostEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleRepost(false, TRACK_URN, eventContextMetadata, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);
        assertEngagementClickEventJson("repost::remove", event.getTimestamp());
    }

    @Test
    public void createsJsonForLikeEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleLike(true, TRACK_URN, eventContextMetadata, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);
        assertEngagementClickEventJson("like::add", event.getTimestamp());
    }

    @Test
    public void createsJsonForUnlikeEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleLike(false, TRACK_URN, eventContextMetadata, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);
        assertEngagementClickEventJson("like::remove", event.getTimestamp());
    }

    @Test
    public void createsJsonFromEngagementClickFromOverflow() throws ApiMapperException {
        EventContextMetadata eventContext = EventContextMetadata.builder()
                                                                .contextScreen("screen")
                                                                .pageName(PAGE_NAME)
                                                                .pageUrn(TRACK_URN)
                                                                .isFromOverflow(true)
                                                                .build();

        final UIEvent event = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("repost::add")
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(TRACK_URN.toString())
                                               .pageName(PAGE_NAME)
                                               .pageUrn(TRACK_URN.toString())
                                               .fromOverflowMenu(true)
        );
    }

    @Test
    public void createsJsonFromEngagementClickFromOverflowWithClickSource() throws ApiMapperException {
        TrackSourceInfo info = new TrackSourceInfo(Screen.STREAM.get(), true);
        info.setSource("stream", "version");

        EventContextMetadata eventContext = EventContextMetadata.builder()
                                                                .contextScreen("screen")
                                                                .pageName(PAGE_NAME)
                                                                .pageUrn(TRACK_URN)
                                                                .isFromOverflow(true)
                                                                .trackSourceInfo(info)
                                                                .build();

        final UIEvent event = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("repost::add")
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(TRACK_URN.toString())
                                               .pageName(PAGE_NAME)
                                               .pageUrn(TRACK_URN.toString())
                                               .clickSource("stream")
                                               .fromOverflowMenu(true)
        );
    }

    @Test
    public void createsJsonForCollectionEvent() throws ApiMapperException {
        final CollectionEvent event = CollectionEvent.forClearFilter();

        jsonDataBuilder.buildForCollectionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName("collection:main")
                                               .clickName("filter_sort::clear")
                                               .clickCategory(CollectionEvent.COLLECTION_CATEGORY));
    }

    @Test
    public void createsJsonForCollectionNavigationEvent() throws ApiMapperException {
        final Urn urn = Urn.forPlaylist(123);
        final CollectionEvent event = CollectionEvent.forRecentlyPlayed(urn, Screen.RECENTLY_PLAYED);

        jsonDataBuilder.buildForCollectionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName("collection:recently_played")
                                               .clickName("item_navigation")
                                               .clickSource("recently_played")
                                               .clickObject(urn.toString()));
    }

    @Test
    public void createsJsonForPlayerOpenUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayerOpen(true);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("player::max")
                                               .clickTrigger("manual"));
    }

    @Test
    public void createsJsonForPlayerCloseUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayerClose(true);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("player::min")
                                               .clickTrigger("manual"));
    }

    @Test
    public void createsJsonForPlayQueueOpenUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayQueueOpen();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("play_queue::max"));
    }

    @Test
    public void createsJsonForPlayQueueCloseUIEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayQueueClose();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("play_queue::min"));
    }

    @Test
    public void createsJsonForPlayQueueRepeatAll() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, RepeatMode.REPEAT_ALL);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(Screen.PLAY_QUEUE.get())
                                               .clickName("repeat::on")
                                               .clickRepeat("all"));
    }

    @Test
    public void createsJsonForPlayQueueRepeatOne() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, RepeatMode.REPEAT_ONE);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(Screen.PLAY_QUEUE.get())
                                               .clickName("repeat::on")
                                               .clickRepeat("one"));
    }

    @Test
    public void createsJsonForPlayQueueRepeatNone() throws ApiMapperException {
        final UIEvent event = UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, RepeatMode.REPEAT_NONE);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .pageName(Screen.PLAY_QUEUE.get())
                                               .clickName("repeat::off"));
    }

    @Test
    public void createsPlayerUpsellImpressionJson() throws Exception {
        UpgradeFunnelEvent impression = UpgradeFunnelEvent.forPlayerImpression(TRACK_URN);

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                                               .pageName("tracks:main")
                                               .pageUrn(TRACK_URN.toString())
                                               .impressionName("consumer_sub_ad")
                                               .impressionObject("soundcloud:tcode:1017"));
    }

    @Test
    public void createsPlayerUpsellClickJson() throws ApiMapperException {
        UpgradeFunnelEvent click = UpgradeFunnelEvent.forPlayerClick(TRACK_URN);

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, click.getTimestamp())
                                               .pageName("tracks:main")
                                               .pageUrn(TRACK_URN.toString())
                                               .clickCategory("consumer_subs")
                                               .clickName("clickthrough::consumer_sub_ad")
                                               .clickObject("soundcloud:tcode:1017"));
    }

    @Test
    public void createsStreamUpsellClickJson() throws Exception {
        UpgradeFunnelEvent click = UpgradeFunnelEvent.forStreamClick();

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, click.getTimestamp())
                                               .clickCategory("consumer_subs")
                                               .clickName("clickthrough::consumer_sub_ad")
                                               .clickObject("soundcloud:tcode:1027")
                                               .pageName("stream:main")
        );
    }

    @Test
    public void createsLikesUpsellImpressionJson() throws Exception {
        UpgradeFunnelEvent impression = UpgradeFunnelEvent.forLikesImpression();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                                               .pageName("collection:likes")
                                               .impressionName("consumer_sub_ad")
                                               .impressionObject("soundcloud:tcode:1009"));
    }

    @Test
    public void createsPlaylistItemUpsellClickJson() throws Exception {
        UpgradeFunnelEvent click = UpgradeFunnelEvent.forPlaylistItemClick(PAGE_NAME, PLAYLIST_URN);

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, click.getTimestamp())
                                               .pageName(PAGE_NAME)
                                               .pageUrn(PLAYLIST_URN.toString())
                                               .clickCategory("consumer_subs")
                                               .clickName("clickthrough::consumer_sub_ad")
                                               .clickObject("soundcloud:tcode:1011"));
    }

    @Test
    public void createsResubscribeClickJson() throws Exception {
        UpgradeFunnelEvent click = UpgradeFunnelEvent.forResubscribeClick();

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, click.getTimestamp())
                                               .pageName("collection:offline_offboarding")
                                               .clickCategory("consumer_subs")
                                               .clickName("clickthrough::consumer_sub_resubscribe")
                                               .clickObject("soundcloud:tcode:4002"));
    }

    @Test
    public void createsScreenEventJson() throws ApiMapperException {
        ScreenEvent screenEvent = ScreenEvent.create(Screen.ACTIVITIES);

        jsonDataBuilder.buildForScreenEvent(screenEvent);

        verify(jsonTransformer).toJson(eq(getEventData("pageview",
                                                       BOOGALOO_VERSION,
                                                       screenEvent.getTimestamp()).pageName(Screen.ACTIVITIES.get())));
    }

    @Test
    public void createsScreenEventJsonWithUuidAndReferringEventWhenHtiEnabled() throws ApiMapperException {
        final String referringEventId = "id";
        ScreenEvent screenEvent = ScreenEvent.create(Screen.ACTIVITIES).putReferringEvent(ReferringEvent.create(referringEventId, ScreenEvent.KIND));
        when(featureFlags.isEnabled(HOLISTIC_TRACKING)).thenReturn(true);

        jsonDataBuilder.buildForScreenEvent(screenEvent);

        verify(jsonTransformer).toJson(eq(getEventData("pageview", BOOGALOO_VERSION, screenEvent.getTimestamp())
                                                  .clientEventId(screenEvent.id())
                                                  .referringEvent(referringEventId, ScreenEvent.KIND)
                                                  .pageName(Screen.ACTIVITIES.get())));
    }

    @Test
    public void createsScreenEventJsonWithQueryUrn() throws ApiMapperException {
        ScreenEvent screenEvent = ScreenEvent.create(Screen.SEARCH_EVERYTHING.get(),
                                                     new SearchQuerySourceInfo(new Urn("soundcloud:search:123"),
                                                                               "query"));


        jsonDataBuilder.buildForScreenEvent(screenEvent);

        verify(jsonTransformer).toJson(eq(getEventData("pageview", BOOGALOO_VERSION, screenEvent.getTimestamp())
                                                  .pageName(Screen.SEARCH_EVERYTHING.get())
                                                  .queryUrn("soundcloud:search:123")));
    }

    @Test
    public void createsScreenEventJsonWithPageUrn() throws ApiMapperException {
        final Urn pageUrn = Urn.forUser(123L);
        ScreenEvent screenEvent = ScreenEvent.create(Screen.SEARCH_EVERYTHING, pageUrn);

        jsonDataBuilder.buildForScreenEvent(screenEvent);

        verify(jsonTransformer).toJson(eq(getEventData("pageview", BOOGALOO_VERSION, screenEvent.getTimestamp())
                                                  .pageName(Screen.SEARCH_EVERYTHING.get())
                                                  .pageUrn(pageUrn.toString())));
    }

    @Test
    public void createsResubscribeImpressionJson() throws Exception {
        UpgradeFunnelEvent impression = UpgradeFunnelEvent.forResubscribeImpression();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                                               .pageName("collection:offline_offboarding")
                                               .impressionName("consumer_sub_resubscribe")
                                               .impressionObject("soundcloud:tcode:4002"));
    }

    @Test
    public void createsUpgradeSuccessImpressionJson() throws Exception {
        UpgradeFunnelEvent impression = UpgradeFunnelEvent.forUpgradeSuccess();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                                               .impressionName("consumer_sub_upgrade_success"));
    }

    @Test
    public void createsPlayQueueShuffleJson() throws Exception {
        UIEvent shuffleEvent = UIEvent.fromPlayQueueShuffle(true);

        jsonDataBuilder.buildForUIEvent(shuffleEvent);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, shuffleEvent.getTimestamp())
                                               .clickName(shuffleEvent.clickName().get().key())
                                               .pageName(shuffleEvent.originScreen().get()));
    }

    @Test
    public void createsPlayQueueReorderJson() throws Exception {
        UIEvent event = UIEvent.fromPlayQueueReorder(Screen.PLAY_QUEUE);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("track_in_play_queue::reorder")
                                               .pageName(event.originScreen().get()));
    }

    @Test
    public void createsPlayQueueRemoveJson() throws Exception {
        UIEvent event = UIEvent.fromPlayQueueRemove(Screen.PLAY_QUEUE);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("track_in_play_queue::remove")
                                               .pageName(event.originScreen().get()));
    }

    @Test
    public void createsPlayQueueRemoveUndoJson() throws Exception {
        UIEvent event = UIEvent.fromPlayQueueRemoveUndo(Screen.PLAY_QUEUE);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName("track_in_play_queue::remove_undo")
                                               .pageName(event.originScreen().get()));
    }

    @Test
    public void createsPlayNextJson() throws Exception {
        final Urn urn = Urn.forTrack(123L);
        final Integer position = 0;
        final Module module = Module.create(Module.STREAM, position);
        final EventContextMetadata eventContextMetadata =
                EventContextMetadata.builder()
                                    .contextScreen("screen")
                                    .pageName(PAGE_NAME)
                                    .module(module)
                                    .build();

        UIEvent event = UIEvent.fromPlayNext(urn, PAGE_NAME, eventContextMetadata);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName(UIEvent.ClickName.PLAY_NEXT.key())
                                               .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                                               .clickObject(urn.toString())
                                               .pageName(PAGE_NAME));
    }

    @Test
    public void createsRecommendedPlaylistJson() throws Exception {
        final TrackSourceInfo sourceInfo = new TrackSourceInfo(PAGE_NAME, false);

        sourceInfo.setQuerySourceInfo(QuerySourceInfo.create(QUERY_POSITION, QUERY_URN));
        sourceInfo.setSource(SOURCE, Strings.EMPTY);
        final EventContextMetadata.Builder contextMetadata = EventContextMetadata.builder().trackSourceInfo(sourceInfo);
        contextMetadata.pageName(PAGE_NAME);
        final Urn clickedPlaylist = Urn.forPlaylist(123L);
        UIEvent event = UIEvent.fromRecommendedPlaylists(clickedPlaylist, contextMetadata.build());

        jsonDataBuilder.buildForUIEvent(event);

        final EventLoggerEventData eventData = getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .clickName("item_navigation")
                .pageName(PAGE_NAME)
                .clickObject(clickedPlaylist.toString())
                .source(SOURCE)
                .clickSource(SOURCE)
                .queryUrn(QUERY_URN.toString())
                .queryPosition(QUERY_POSITION);
        verify(jsonTransformer).toJson(eventData);
    }

    @Test
    public void createsLegacySearchEvent() throws Exception {
        SearchEvent event = SearchEvent.searchStart(Screen.SEARCH_MAIN, new SearchQuerySourceInfo(QUERY_URN, SEARCH_QUERY));

        jsonDataBuilder.buildForSearchEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .queryUrn(QUERY_URN.toString())
                                               .clickName(SearchEvent.ClickName.SEARCH.key)
                                               .searchQuery(SEARCH_QUERY)
                                               .pageName(Screen.SEARCH_MAIN.get()));
    }

    @Test
    public void createsSearchInitJson() throws Exception {
        SearchEvent event = SearchEvent.searchFormulationInit(Screen.SEARCH_MAIN,
                                                              SEARCH_QUERY);

        jsonDataBuilder.buildForSearchEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName(SearchEvent.ClickName.FORMULATION_INIT.key)
                                               .searchQuery(SEARCH_QUERY)
                                               .pageName(Screen.SEARCH_MAIN.get()));
    }

    @Test
    public void createsSearchEndJson() throws Exception {
        SearchEvent event = SearchEvent.searchFormulationEnd(Screen.SEARCH_MAIN,
                                                             SEARCH_QUERY,
                                                             Optional.of(QUERY_URN),
                                                             Optional.of(QUERY_POSITION));

        jsonDataBuilder.buildForSearchEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName(SearchEvent.ClickName.FORMULATION_END.key)
                                               .searchQuery(SEARCH_QUERY)
                                               .pageName(Screen.SEARCH_MAIN.get())
                                               .queryUrn(QUERY_URN.toString())
                                               .queryPosition(QUERY_POSITION));
    }

    @Test
    public void createsSearchLocalSuggestionJson() throws Exception {
        SearchEvent event = SearchEvent.tapLocalSuggestionOnScreen(Screen.SEARCH_MAIN, CLICK_OBJECT_URN, SEARCH_QUERY, QUERY_POSITION);

        jsonDataBuilder.buildForSearchEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                                               .clickName(SearchEvent.ClickName.ITEM_NAVIGATION.key)
                                               .clickObject(CLICK_OBJECT_URN.toString())
                                               .searchQuery(SEARCH_QUERY)
                                               .clickSource(SearchEvent.ClickSource.AUTOCOMPLETE.key)
                                               .searchQuery(SEARCH_QUERY)
                                               .pageName(Screen.SEARCH_MAIN.get())
                                               .queryPosition(QUERY_POSITION));
    }

    private void assertEngagementClickEventJson(String engagementName, long timestamp) throws ApiMapperException {
        verify(jsonTransformer).toJson(getEngagementEventData(engagementName, timestamp)
        );
    }

    private EventLoggerEventData getEngagementEventData(String engagementName, long timestamp) {
        return getEventData("click", BOOGALOO_VERSION, timestamp)
                .clickName(engagementName)
                .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                .clickObject(TRACK_URN.toString())
                .clickSource(SOURCE)
                .clickSourceUrn(STATION_URN.toString())
                .queryUrn(QUERY_URN.toString())
                .queryPosition(QUERY_POSITION)
                .pageName(PAGE_NAME)
                .pageUrn(TRACK_URN.toString());
    }


    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, long timestamp) {
        return new EventLoggerEventDataV1(eventName,
                                          boogalooVersion,
                                          CLIENT_ID,
                                          UDID,
                                          LOGGED_IN_USER.toString(),
                                          timestamp,
                                          ConnectionType.WIFI.getValue(),
                                          String.valueOf(APP_VERSION_CODE));
    }

    private EventContextMetadata createEventContextMetadata() {
        return EventContextMetadata.builder()
                                   .contextScreen("screen")
                                   .trackSourceInfo(trackSourceInfo)
                                   .pageName(PAGE_NAME)
                                   .pageUrn(TRACK_URN)
                                   .build();
    }

    private TrackSourceInfo createTrackSourceInfo() {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.LIKES.get(), true);
        trackSourceInfo.setSource(SOURCE, "0.0");
        trackSourceInfo.setStationSourceInfo(STATION_URN, StationsSourceInfo.create(QUERY_URN));
        trackSourceInfo.setQuerySourceInfo(QuerySourceInfo.create(QUERY_POSITION, QUERY_URN));

        return trackSourceInfo;
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs(TrackItem track,
                                                TrackSourceInfo trackSourceInfo,
                                                long progress,
                                                boolean isOfflineTrack) {
        return PlaybackSessionEventArgs.create(track, trackSourceInfo, progress, PROTOCOL,
                                               PLAYER_TYPE, isOfflineTrack, false, CLIENT_EVENT_ID, PLAY_ID);
    }
}
