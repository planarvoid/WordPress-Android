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
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.OfflineSyncTrackingEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;
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
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn CREATOR_URN = Urn.forUser(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Urn STATION_URN = Urn.forTrackStation(123L);
    private static final String CONSUMER_SUBS_PLAN = "THE HIGHEST TIER IMAGINABLE";
    public static final Urn QUERY_URN = new Urn("soundcloud:radio:6d2547a");
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
        when(featureOperations.getPlan()).thenReturn(CONSUMER_SUBS_PLAN);
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(PlayableProperty.PLAY_DURATION))
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

        verify(jsonTransformer).toJson(getEventData("audio", "v1.7.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.PLAY_DURATION))
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
    public void createsJsonForLikesToOfflineAddEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromAddOfflineLikes(PAGE_NAME);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("likes_to_offline::add")
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForLikesToOfflineRemoveEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromRemoveOfflineLikes(PAGE_NAME);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("likes_to_offline::remove")
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForCollectionToOfflineAddEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleOfflineCollection(true);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("collection_to_offline::add"));
    }

    @Test
    public void createsJsonForCollectionToOfflineRemoveEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleOfflineCollection(false);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("collection_to_offline::remove"));
    }

    @Test
    public void createsJsonForPlaylistToOfflineAddEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromAddOfflinePlaylist(PAGE_NAME, PLAYLIST_URN, null);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("playlist_to_offline::add")
                .clickObject(String.valueOf(PLAYLIST_URN))
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromRemoveOfflinePlaylist(PAGE_NAME, PLAYLIST_URN, null);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("playlist_to_offline::remove")
                .clickObject(String.valueOf(PLAYLIST_URN))
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEventForPromotedItem() throws ApiMapperException {
        PromotedListItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final UIEvent event = UIEvent.fromRemoveOfflinePlaylist(PAGE_NAME, PLAYLIST_URN, promotedSourceInfo);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .clickName("playlist_to_offline::remove")
                .clickObject(String.valueOf(PLAYLIST_URN))
                .pageName(PAGE_NAME)
                .adUrn(item.getAdUrn())
                .monetizationType("promoted")
                .promotedBy(item.getPromoterUrn().get().toString()));
    }

    @Test
    public void createJsonFromOfflineSyncStartEventWithPlaylistTrackContext() throws ApiMapperException {
        final TrackingMetadata trackContext = new TrackingMetadata(CREATOR_URN, true, false);
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromStarted(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflineSyncEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", "v1.7.0", event.getTimestamp())
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
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromFailed(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflineSyncEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", "v1.7.0", event.getTimestamp())
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
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromCancelled(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflineSyncEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", "v1.7.0", event.getTimestamp())
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
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromCompleted(TRACK_URN, trackContext);

        jsonDataBuilder.buildForOfflineSyncEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", "v1.7.0", event.getTimestamp())
                        .track(TRACK_URN)
                        .trackOwner(CREATOR_URN)
                        .inOfflinePlaylist(false)
                        .inOfflineLikes(true)
                        .eventStage("complete")
        );
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

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
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

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
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

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
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

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", event.getTimestamp())
                .pageName("collection:overview")
                .clickName("filter_sort::clear")
                .clickCategory(EventLoggerClickCategories.COLLECTION));
    }

    @Test
    public void createsPlayerUpsellImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forPlayerImpression(TRACK_URN);

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v1.7.0", impression.getTimestamp())
                .pageName("tracks:main")
                .pageUrn(TRACK_URN.toString())
                .impressionName("consumer_sub_ad")
                .impressionObject("soundcloud:tcode:1017"));
    }

    @Test
    public void createsPlayerUpsellClickJson() throws ApiMapperException {
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forPlayerClick(TRACK_URN);

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", click.getTimestamp())
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

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", click.getTimestamp())
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

        verify(jsonTransformer).toJson(getEventData("impression", "v1.7.0", impression.getTimestamp())
                .pageName("collection:likes")
                .impressionName("consumer_sub_ad")
                .impressionObject("soundcloud:tcode:1009"));
    }

    @Test
    public void createsPlaylistItemUpsellClickJson() throws Exception {
        UpgradeTrackingEvent click = UpgradeTrackingEvent.forPlaylistItemClick(PAGE_NAME, PLAYLIST_URN);

        jsonDataBuilder.buildForUpsell(click);

        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", click.getTimestamp())
                .pageName(PAGE_NAME)
                .pageUrn(PLAYLIST_URN.toString())
                .clickCategory("consumer_subs")
                .clickName("clickthrough::consumer_sub_ad")
                .clickObject("soundcloud:tcode:1011"));
    }

    @Test
    public void createsUpgradeSuccessImpressionJson() throws Exception {
        UpgradeTrackingEvent impression = UpgradeTrackingEvent.forUpgradeSuccess();

        jsonDataBuilder.buildForUpsell(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v1.7.0", impression.getTimestamp())
                .impressionName("consumer_sub_upgrade_success"));
    }

    private void assertEngagementClickEventJson(String engagementName, long timestamp) throws ApiMapperException {
        verify(jsonTransformer).toJson(getEventData("click", "v1.7.0", timestamp)
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
