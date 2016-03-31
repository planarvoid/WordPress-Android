package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdDeliveryEvent.AdsReceived;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;

public class EventLoggerV1JsonDataBuilderTest extends AndroidUnitTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final String UUID = "uuid";
    private static final int APP_VERSION_CODE = 386;
    private static final int CLIENT_ID = 3152;
    private static final String BOOGALOO_VERSION = "v1.14.0";
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn CREATOR_URN = Urn.forUser(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Urn STATION_URN = Urn.forTrackStation(123L);
    private static final Plan CONSUMER_SUBS_PLAN = Plan.HIGH_TIER;
    public static final Urn QUERY_URN = new Urn("soundcloud:radio:6d2547a");
    public static final Urn AD_URN = Urn.forAd("dfp", "123");
    private static final String PAGE_NAME = "page_name";
    public static final String SOURCE = "stations";

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private FeatureOperations featureOperations;
    @Mock private NetworkConnectionHelper connectionHelper;

    private EventLoggerV1JsonDataBuilder jsonDataBuilder;
    private final TrackSourceInfo trackSourceInfo = createTrackSourceInfo();
    private EventContextMetadata eventContextMetadata = createEventContextMetadata();
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:click:urn"));
    private final EntityMetadata entityMetadata = EntityMetadata.from(PropertySet.create());

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerV1JsonDataBuilder(context().getResources(),
                deviceHelper, connectionHelper, accountOperations, jsonTransformer, featureOperations, experimentOperations);

        when(connectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.WIFI);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUdid()).thenReturn(UDID);
        when(deviceHelper.getAppVersionCode()).thenReturn(APP_VERSION_CODE);
        when(featureOperations.getCurrentPlan()).thenReturn(CONSUMER_SUBS_PLAN);
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true, false, UUID);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .reposter(Urn.forUser(456L))
                .localStoragePlayback(true)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inOfflinePlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEventForStationsSeedTrack() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true, false, UUID);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));
        trackSourceInfo.setStationSourceInfo(STATION_URN, StationsSourceInfo.create(Urn.NOT_SET));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .reposter(Urn.forUser(456L))
                .localStoragePlayback(true)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceUrn(STATION_URN.toString())
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEventForStations() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true, false, UUID);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));
        trackSourceInfo.setStationSourceInfo(STATION_URN, StationsSourceInfo.create(new Urn("soundcloud:radio:123-456")));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .reposter(Urn.forUser(456L))
                .localStoragePlayback(true)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceUrn(STATION_URN.toString())
                .queryUrn("soundcloud:radio:123-456")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL));
    }

    @Test
    public void createsAudioPauseEventJson() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false, UUID);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_CONCURRENT_STREAMING, false, UUID);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("pause")
                .playheadPosition(123L)
                .source("source")
                .sourceVersion("source-version")
                .inOfflinePlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL)
                .reason("concurrent_streaming"));
    }

    @Test
    public void createsAudioPauseEventJsonForStationsForSeedTrack() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setStationSourceInfo(STATION_URN, StationsSourceInfo.create(Urn.NOT_SET));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false, UUID);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR, false, UUID);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("pause")
                .playheadPosition(123L)
                .source("source")
                .sourceUrn(STATION_URN.toString())
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL)
                .reason("playback_error"));
    }

    @Test
    public void createsAudioPauseEventJsonForStations() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setStationSourceInfo(STATION_URN, StationsSourceInfo.create(new Urn("soundcloud:radio:123-456")));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false, UUID);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR, false, UUID);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("pause")
                .playheadPosition(123L)
                .source("source")
                .sourceUrn(STATION_URN.toString())
                .queryUrn("soundcloud:radio:123-456")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL)
                .reason("playback_error"));
    }

    @Test
    public void createsAudioEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false, UUID);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(TrackProperty.FULL_DURATION))
                .track(audioAdTrack.get(TrackProperty.URN))
                .trackOwner(audioAdTrack.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .policy("ALLOW")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inOfflinePlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL)
                .adUrn(audioAd.getAdUrn().toString())
                .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsAudioPauseEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent playbackSessionEvent = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true, false, UUID);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, playbackSessionEvent, 12L, 456L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING, true, UUID);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));
        trackSourceInfo.setSearchQuerySourceInfo(searchQuerySourceInfo);

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(TrackProperty.FULL_DURATION))
                .track(audioAdTrack.get(TrackProperty.URN))
                .trackOwner(audioAdTrack.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(true)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .action("pause")
                .playheadPosition(12L)
                .reason("buffer_underrun")
                .trigger("manual")
                .source("source")
                .sourceVersion("source-version")
                .inOfflinePlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .adUrn(audioAd.getAdUrn().toString())
                .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                .monetizationType("audio_ad")
                .policy("ALLOW")
                .queryUrn("some:search:urn")
                .queryPosition(5)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL));
    }

    @Test
    public void createAudioEventJsonWithAdMetadataForPromotedTrackPlay() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PromotedSourceInfo promotedSource = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.of(Urn.forUser(123L)), Arrays.asList("promoted1", "promoted2"));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo, 12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false, UUID);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withPromotedTrack(promotedSource));

        verify(jsonTransformer).toJson(getEventData("audio", BOOGALOO_VERSION, event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(TrackProperty.FULL_DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inOfflinePlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .uuid(UUID)
                .monetizationModel(TestPropertySets.MONETIZATION_MODEL)
                .adUrn("ad:urn:123")
                .monetizationType("promoted")
                .promotedBy("soundcloud:users:123"));
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
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromDisableCollectionSync(PAGE_NAME, Optional.of(PLAYLIST_URN));

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .clickName("automatic_collection_sync::disable")
                .clickCategory("consumer_subs")
                .clickObject(PLAYLIST_URN.toString())
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineAddEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromAddOfflinePlaylist(PAGE_NAME, PLAYLIST_URN, null);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .clickCategory("consumer_subs")
                .clickName("playlist_to_offline::add")
                .clickObject(String.valueOf(PLAYLIST_URN))
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEvent() throws ApiMapperException {
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflinePlaylist(PAGE_NAME, PLAYLIST_URN, null);

        jsonDataBuilder.buildForOfflineInteractionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .clickCategory("consumer_subs")
                .clickName("playlist_to_offline::remove")
                .clickObject(String.valueOf(PLAYLIST_URN))
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEventForPromotedItem() throws ApiMapperException {
        final PromotedListItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflinePlaylist(PAGE_NAME, PLAYLIST_URN, promotedSourceInfo);

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
    public void createsJsonFromAdDeliveryEvent() throws ApiMapperException {
        final AdsReceived adsReceived = new AdsReceived(AD_URN, Urn.NOT_SET, Urn.NOT_SET);
        final AdDeliveryEvent event = AdDeliveryEvent.adDelivered(TRACK_URN, AD_URN, "endpoint", adsReceived, false, true, true);
        when(jsonTransformer.toJson(adsReceived.ads)).thenReturn("{ads-received}");

        jsonDataBuilder.buildForAdDelivery(event);

        verify(jsonTransformer).toJson(adsReceived.ads);
        verify(jsonTransformer).toJson(getEventData("ad_delivery", BOOGALOO_VERSION, event.getTimestamp())
                .adsRequested(true)
                .adsReceived("{ads-received}")
                .adsRequestSuccess(true)
                .adUrn(AD_URN.toString())
                .adOptimized(false)
                .monetizedObject(TRACK_URN.toString())
                .inForeground(true)
                .playerVisible(true)
                .adsEndpoint("endpoint"));
    }

    @Test
    public void createsJsonFromAdDeliveryEventWithNoSelectedAdUrn() throws ApiMapperException {
        final AdsReceived adsReceived = new AdsReceived(AD_URN, Urn.NOT_SET, Urn.NOT_SET);
        final AdDeliveryEvent event = AdDeliveryEvent.adDelivered(TRACK_URN, Urn.NOT_SET, "endpoint", adsReceived, true, true, false);
        when(jsonTransformer.toJson(adsReceived.ads)).thenReturn("{ads-received}");

        jsonDataBuilder.buildForAdDelivery(event);

        verify(jsonTransformer).toJson(adsReceived.ads);
        verify(jsonTransformer).toJson(getEventData("ad_delivery", BOOGALOO_VERSION, event.getTimestamp())
                .adsRequested(true)
                .adsReceived("{ads-received}")
                .adsRequestSuccess(true)
                .adOptimized(true)
                .monetizedObject(TRACK_URN.toString())
                .inForeground(false)
                .playerVisible(true)
                .adsEndpoint("endpoint"));
    }

    @Test
    public void createsJsonFromFailedAdDeliveryEvent() throws ApiMapperException {
        final AdDeliveryEvent event = AdDeliveryEvent.adsRequestFailed(TRACK_URN, "endpoint", true, false);

        jsonDataBuilder.buildForAdDelivery(event);

        verify(jsonTransformer).toJson(getEventData("ad_delivery", BOOGALOO_VERSION, event.getTimestamp())
                .adsRequested(true)
                .adsRequestSuccess(false)
                .monetizedObject(TRACK_URN.toString())
                .inForeground(false)
                .playerVisible(true)
                .adsEndpoint("endpoint"));
    }

    @Test
    public void createsJsonForFirstQuartileAdPlaybackProgressEvent() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forFirstQuartile(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForAdProgressQuartileEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .monetizedObject(TRACK_URN.toString())
                .adUrn("dfp:ads:905")
                .pageName("collection:likes")
                .monetizationType("video_ad")
                .clickName("ad::first_quartile"));
    }

    @Test
    public void createsJsonForSecondQuartileAdPlaybackProgressEvent() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forSecondQuartile(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForAdProgressQuartileEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .monetizedObject(TRACK_URN.toString())
                .adUrn("dfp:ads:905")
                .pageName("collection:likes")
                .monetizationType("video_ad")
                .clickName("ad::second_quartile"));
    }

    @Test
    public void createsJsonForThirdQuartileAdPlaybackProgressEvent() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forThirdQuartile(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

        jsonDataBuilder.buildForAdProgressQuartileEvent(event);

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
        final UIEvent event = UIEvent.fromSkipVideoAdClick(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

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
        final UIEvent event = UIEvent.fromVideoAdClickThrough(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo);

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
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forPlay(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo, Player.StateTransition.DEFAULT);

        jsonDataBuilder.buildForAdImpression(event);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, event.getTimestamp())
                .monetizedObject(TRACK_URN.toString())
                .adUrn("dfp:ads:905")
                .pageName("collection:likes")
                .monetizationType("video_ad")
                .impressionName("video_ad_impression"));
    }

    @Test
    public void createsJsonForVideoAdFinish() throws ApiMapperException {
        final AdPlaybackSessionEvent event = AdPlaybackSessionEvent.forPlay(AdFixtures.getVideoAd(TRACK_URN), trackSourceInfo, Player.StateTransition.DEFAULT);

        jsonDataBuilder.buildForAdFinished(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                .monetizedObject(TRACK_URN.toString())
                .adUrn("dfp:ads:905")
                .pageName("collection:likes")
                .monetizationType("video_ad")
                .clickName("ad::finish"));
    }

    @Test
    public void createsJsonForShareEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromShare(TRACK_URN, eventContextMetadata, null, entityMetadata);

        jsonDataBuilder.buildForUIEvent(event);
        assertEngagementClickEventJson("share", event.getTimestamp());
    }

    @Test
    public void addsCurrentExperimentJson() throws ApiMapperException {
        final UIEvent event = UIEvent.fromShare(TRACK_URN, eventContextMetadata, null, entityMetadata);
        setupExperiments();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, event.getTimestamp())
                        .clickName("share")
                        .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                        .clickObject(TRACK_URN.toString())
                        .clickSource(SOURCE)
                        .clickSourceUrn(STATION_URN.toString())
                        .queryUrn(QUERY_URN.toString())
                        .pageName(PAGE_NAME)
                        .experiment("exp_android_listening", 2345)
                        .experiment("exp_android_ui", 3456)
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
                .pageName("collection:overview")
                .clickName("filter_sort::clear")
                .clickCategory(EventLoggerClickCategories.COLLECTION));
    }

    @Test
    public void createsPlayerUpsellImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forPlayerImpression(TRACK_URN);

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                .pageName("tracks:main")
                .pageUrn(TRACK_URN.toString())
                .impressionName("consumer_sub_ad")
                .impressionObject("soundcloud:tcode:1017"));
    }

    @Test
    public void createsPlayerUpsellClickJson() throws ApiMapperException {
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forPlayerClick(TRACK_URN);

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
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forStreamClick();

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
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forLikesImpression();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                .pageName("collection:likes")
                .impressionName("consumer_sub_ad")
                .impressionObject("soundcloud:tcode:1009"));
    }

    @Test
    public void createsPlaylistItemUpsellClickJson() throws Exception {
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forPlaylistItemClick(PAGE_NAME, PLAYLIST_URN);

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
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forResubscribeClick();

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, click.getTimestamp())
                .pageName("collection:offline_offboarding")
                .clickCategory("consumer_subs")
                .clickName("clickthrough::consumer_sub_resubscribe")
                .clickObject("soundcloud:tcode:4002"));
    }

    @Test
    public void createsResubscribeImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forResubscribeImpression();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                .pageName("collection:offline_offboarding")
                .impressionName("consumer_sub_resubscribe")
                .impressionObject("soundcloud:tcode:4002"));
    }

    @Test
    public void createsUpgradeSuccessImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forUpgradeSuccess();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", BOOGALOO_VERSION, impression.getTimestamp())
                .impressionName("consumer_sub_upgrade_success"));
    }

    private void assertEngagementClickEventJson(String engagementName, long timestamp) throws ApiMapperException {
        verify(jsonTransformer).toJson(getEventData("click", BOOGALOO_VERSION, timestamp)
                        .clickName(engagementName)
                        .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                        .clickObject(TRACK_URN.toString())
                        .clickSource(SOURCE)
                        .clickSourceUrn(STATION_URN.toString())
                        .queryUrn(QUERY_URN.toString())
                        .pageName(PAGE_NAME)
                        .pageUrn(TRACK_URN.toString())
        );
    }

    private void setupExperiments() {
        HashMap<String, Integer> activeExperiments = new HashMap<>();
        activeExperiments.put("exp_android_listening", 2345);
        activeExperiments.put("exp_android_ui", 3456);
        when(experimentOperations.getTrackingParams()).thenReturn(activeExperiments);
    }

    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, long timestamp) {
        return new EventLoggerEventDataV1(eventName, boogalooVersion, CLIENT_ID, UDID, LOGGED_IN_USER.toString(), timestamp, ConnectionType.WIFI.getValue(), String.valueOf(APP_VERSION_CODE));
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

        return trackSourceInfo;
    }
}
