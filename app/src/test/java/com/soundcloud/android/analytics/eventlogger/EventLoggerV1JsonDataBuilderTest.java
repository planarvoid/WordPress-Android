package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineTrackContext;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.PromotedListItem;
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
import java.util.Collections;
import java.util.HashMap;

public class EventLoggerV1JsonDataBuilderTest extends AndroidUnitTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final String APP_VERSION = "15.09.11-release";
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn CREATOR_URN = Urn.forUser(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Urn STATION_URN = Urn.forTrackStation(123L);
    private static final String CONSUMER_SUBS_PLAN = "THE HIGHEST TIER IMAGINABLE";
    private static final String PAGE_NAME = "page_name";

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private FeatureOperations featureOperations;
    @Mock private NetworkConnectionHelper connectionHelper;

    private EventLoggerV1JsonDataBuilder jsonDataBuilder;
    private final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.LIKES.get(), true);
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:click:urn"));
    private final PlayableMetadata playableMetadata = PlayableMetadata.from(PropertySet.create());

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerV1JsonDataBuilder(context().getResources(),
                deviceHelper, connectionHelper, accountOperations, jsonTransformer, featureOperations, experimentOperations);

        when(connectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.WIFI);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUdid()).thenReturn(UDID);
        when(deviceHelper.getAppVersion()).thenReturn(APP_VERSION);
        when(featureOperations.getPlan()).thenReturn(CONSUMER_SUBS_PLAN);
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
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
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA"));
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEventForStations() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setReposter(Urn.forUser(456L));
        trackSourceInfo.setOriginStation(STATION_URN);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .reposter(Urn.forUser(456L))
                .localStoragePlayback(true)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .queryUrn(STATION_URN.toString())
                .source("source")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA"));
    }

    @Test
    public void createsAudioPauseEventJson() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR, false);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("pause")
                .playheadPosition(123L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .reason("playback_error"));
    }

    @Test
    public void createsAudioPauseEventJsonForStations() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));
        trackSourceInfo.setOriginStation(STATION_URN);

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR, false);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("pause")
                .playheadPosition(123L)
                .queryUrn(STATION_URN.toString())
                .source("source")
                .sourceVersion("source-version")
                .protocol("hls")
                .playerType("PLAYA")
                .reason("playback_error"));
    }

    @Test
    public void createsAudioEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(PlayableProperty.DURATION))
                .track(audioAdTrack.get(TrackProperty.URN))
                .trackOwner(audioAdTrack.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .adUrn(audioAd.get(AdProperty.AD_URN))
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsAudioPauseEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L), Urn.forUser(789L));
        final PlaybackSessionEvent playbackSessionEvent = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, true);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, playbackSessionEvent, 12L, 456L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING, true);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));
        trackSourceInfo.setSearchQuerySourceInfo(searchQuerySourceInfo);

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(PlayableProperty.DURATION))
                .track(audioAdTrack.get(TrackProperty.URN))
                .trackOwner(audioAdTrack.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(true)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .action("pause")
                .playheadPosition(12L)
                .reason("buffering")
                .trigger("manual")
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .adUrn(audioAd.get(AdProperty.AD_URN))
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .monetizationType("audio_ad")
                .queryUrn("some:search:urn")
                .queryPosition(5));
    }

    @Test
    public void createAudioEventJsonWithAdMetadataForPromotedTrackPlay() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PromotedSourceInfo promotedSource = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.of(Urn.forUser(123L)), Arrays.asList("promoted1", "promoted2"));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo, 12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withPromotedTrack(promotedSource));

        verify(jsonTransformer).toJson(getEventData("audio", "v1.4.0", event.getTimestamp())
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trackOwner(track.get(TrackProperty.CREATOR_URN))
                .localStoragePlayback(false)
                .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition(2)
                .protocol("hls")
                .playerType("PLAYA")
                .adUrn("ad:urn:123")
                .monetizationType("promoted")
                .promotedBy("soundcloud:users:123"));
    }

    @Test
    public void createsJsonForLikesToOfflineAddEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromAddOfflineLikes(PAGE_NAME);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("likes_to_offline::add")
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForLikesToOfflineRemoveEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromRemoveOfflineLikes(PAGE_NAME);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("likes_to_offline::remove")
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForCollectionToOfflineAddEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleOfflineCollection(true);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("collection_to_offline::add"));
    }

    @Test
    public void createsJsonForCollectionToOfflineRemoveEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromToggleOfflineCollection(false);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("collection_to_offline::remove"));
    }

    @Test
    public void createsJsonForPlaylistToOfflineAddEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromAddOfflinePlaylist(PAGE_NAME, Urn.forPlaylist(123L), null);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("playlist_to_offline::add")
                .clickObject(String.valueOf(Urn.forPlaylist(123L)))
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromRemoveOfflinePlaylist(PAGE_NAME, Urn.forPlaylist(123L), null);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("playlist_to_offline::remove")
                .clickObject(String.valueOf(Urn.forPlaylist(123L)))
                .pageName(PAGE_NAME));
    }

    @Test
    public void createsJsonForPlaylistToOfflineRemoveEventForPromotedItem() throws ApiMapperException {
        PromotedListItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(item);
        final UIEvent event = UIEvent.fromRemoveOfflinePlaylist(PAGE_NAME, Urn.forPlaylist(123L), promotedSourceInfo);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .clickName("playlist_to_offline::remove")
                .clickObject(String.valueOf(Urn.forPlaylist(123L)))
                .pageName(PAGE_NAME)
                .adUrn(item.getAdUrn())
                .monetizationType("promoted")
                .promotedBy(item.getPromoterUrn().get().toString()));
    }

    @Test
    public void createJsonFromOfflineSyncEvent() throws ApiMapperException {
        final OfflineTrackContext trackContext = OfflineTrackContext.create(TRACK_URN, CREATOR_URN, Collections.EMPTY_LIST, false);
        final OfflineSyncEvent event = OfflineSyncEvent.fromDesync(trackContext);

        jsonDataBuilder.buildForOfflineSyncEvent(event);

        verify(jsonTransformer).toJson(getEventData("offline_sync", "v1.4.0", event.getTimestamp())
                        .consumerSubsPlan(CONSUMER_SUBS_PLAN)
                        .track(TRACK_URN)
                        .trackOwner(CREATOR_URN)
                        .inPlaylist(false)
                        .inLikes(false)
                        .appVersion(APP_VERSION)
                        .eventType("desync")
                        .eventStage("complete")
        );
    }

    @Test
    public void createsJsonForShareEvent() throws ApiMapperException {
        final UIEvent event = UIEvent.fromShare("screen", PAGE_NAME, TRACK_URN, TRACK_URN, null, playableMetadata);

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                        .clickName("share")
                        .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                        .clickObject(TRACK_URN.toString())
                        .pageName(PAGE_NAME)
                        .pageUrn(TRACK_URN.toString())
        );
    }

    @Test
    public void addsCurrentExperimentJson() throws ApiMapperException {
        final UIEvent event = UIEvent.fromShare("screen", PAGE_NAME, TRACK_URN, TRACK_URN, null, playableMetadata);
        setupExperiments();

        jsonDataBuilder.buildForUIEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                        .clickName("share")
                        .clickCategory(EventLoggerClickCategories.ENGAGEMENT)
                        .clickObject(TRACK_URN.toString())
                        .pageName(PAGE_NAME)
                        .experiment("exp_android_listening", 2345)
                        .experiment("exp_android_ui", 3456)
                        .pageUrn(TRACK_URN.toString())
        );
    }

    private void setupExperiments() {
        HashMap<String, Integer> activeExperiments = new HashMap<>();
        activeExperiments.put("exp_android_listening", 2345);
        activeExperiments.put("exp_android_ui", 3456);
        when(experimentOperations.getTrackingParams()).thenReturn(activeExperiments);
    }

    @Test
    public void createsJsonForCollectionEvent() throws ApiMapperException {
        final CollectionEvent event = CollectionEvent.forClearFilter();

        jsonDataBuilder.buildForCollectionEvent(event);

        verify(jsonTransformer).toJson(getEventData("click", "v1.4.0", event.getTimestamp())
                .pageName("collection:overview")
                .clickName("filter_sort::clear")
                .clickCategory(EventLoggerClickCategories.COLLECTION));
    }

    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, long timestamp) {
        return new EventLoggerEventDataV1(eventName, boogalooVersion, 3152, UDID, LOGGED_IN_USER.toString(), timestamp, ConnectionType.WIFI.getValue());
    }

}
