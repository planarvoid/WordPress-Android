package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

public class EventLoggerV1JsonDataBuilderTest extends AndroidUnitTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;

    private EventLoggerV1JsonDataBuilder jsonDataBuilder;
    private final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.SIDE_MENU_LIKES.get(), true);
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:click:urn"));

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerV1JsonDataBuilder(context().getResources(), experimentOperations,
                deviceHelper, accountOperations, jsonTransformer);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUdid()).thenReturn(UDID);
    }

    @Test
    public void createsAudioEventJsonForAudioPlaybackEvent() throws ApiMapperException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", String.valueOf(event.getTimestamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g"));
    }

    @Test
    public void createsAudioPauseEventJson() throws ApiMapperException, CreateModelException {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        final PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo,
                0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(track, LOGGED_IN_USER, trackSourceInfo,
                playEvent, 123L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_ERROR);

        jsonDataBuilder.buildForAudioEvent(event);

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", String.valueOf(event.getTimestamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trigger("manual")
                .action("pause")
                .playheadPosition(123L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition("2")
                .protocol("hls")
                .playerType("PLAYA")
                .reason("playback_error")
                .connectionType("3g"));
    }

    @Test
    public void createsAudioEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(PLAYLIST_URN, 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", String.valueOf(event.getTimestamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(PlayableProperty.DURATION))
                .track(audioAdTrack.get(TrackProperty.URN))
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn(audioAd.get(AdProperty.AUDIO_AD_URN))
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsAudioPauseEventJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final PlaybackSessionEvent playbackSessionEvent = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        final PlaybackSessionEvent event = PlaybackSessionEvent.forStop(audioAdTrack, LOGGED_IN_USER, trackSourceInfo, playbackSessionEvent, 12L, 456L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));
        trackSourceInfo.setSearchQuerySourceInfo(searchQuerySourceInfo);

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", String.valueOf(event.getTimestamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(audioAdTrack.get(PlayableProperty.DURATION))
                .track(audioAdTrack.get(TrackProperty.URN))
                .action("pause")
                .playheadPosition(12L)
                .reason("buffering")
                .trigger("manual")
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn(audioAd.get(AdProperty.AUDIO_AD_URN))
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .monetizationType("audio_ad")
                .queryUrn("some:search:urn")
                .queryPosition("5"));
    }

    @Test
    public void createAudioEventJsonWithAdMetadataForPromotedTrackPlay() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final PromotedSourceInfo promotedSource = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.of(Urn.forUser(123L)), Arrays.asList("promoted1", "promoted2"));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(track, LOGGED_IN_USER, trackSourceInfo, 12L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);

        trackSourceInfo.setSource("source", "source-version");
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withPromotedTrack(promotedSource));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", String.valueOf(event.getTimestamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .trackLength(track.get(PlayableProperty.DURATION))
                .track(track.get(TrackProperty.URN))
                .trigger("manual")
                .action("play")
                .playheadPosition(12L)
                .source("source")
                .sourceVersion("source-version")
                .inPlaylist(PLAYLIST_URN)
                .playlistPosition("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn("ad:urn:123")
                .monetizationType("promoted")
                .promotedBy("soundcloud:users:123"));
    }

    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, String timestamp) {
        return new EventLoggerEventData(eventName, boogalooVersion, "3152", UDID, LOGGED_IN_USER.toString(), timestamp);
    }

}
