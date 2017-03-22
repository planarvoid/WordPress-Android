package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.properties.Flag.HOLISTIC_TRACKING;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.InterstitialAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class EventLoggerJsonDataBuilderTest extends AndroidUnitTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final long TIMESTAMP = new Date().getTime();
    private static final String CDN_URL = "host.com";
    private static final String MEDIA_TYPE = "mp3";
    private static final int BIT_RATE = 128000;
    private static final Optional<String> DETAILS_JSON = Optional.of("{\"some\":{\"key\": 1234}}");

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private FeatureFlags featureFlags;

    private EventLoggerJsonDataBuilder jsonDataBuilder;
    private final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.LIKES.get(), true);

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerJsonDataBuilder(context().getResources(),
                                                         experimentOperations,
                                                         deviceHelper,
                                                         accountOperations,
                                                         jsonTransformer,
                                                         featureFlags);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUdid()).thenReturn(UDID);
        when(featureFlags.isEnabled(HOLISTIC_TRACKING)).thenReturn(false);
    }

    @Test
    public void createsJsonForLeaveBehindImpression() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));

        jsonDataBuilder.build(AdOverlayTrackingEvent.forImpression(TIMESTAMP,
                                                                   leaveBehindAd,
                                                                   monetizedTrack,
                                                                   LOGGED_IN_USER,
                                                                   trackSourceInfo));

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
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TIMESTAMP,
                                                                                  interstitialAd,
                                                                                  monetizedTrack,
                                                                                  LOGGED_IN_USER,
                                                                                  trackSourceInfo);
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
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(TIMESTAMP,
                                                                             leaveBehindAd,
                                                                             monetizedTrack,
                                                                             LOGGED_IN_USER,
                                                                             trackSourceInfo);

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
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(TIMESTAMP,
                                                                             interstitialAd,
                                                                             monetizedTrack,
                                                                             LOGGED_IN_USER,
                                                                             trackSourceInfo);

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
        final VisualAdImpressionEvent event = VisualAdImpressionEvent.create(audioAd,
                                                                             LOGGED_IN_USER,
                                                                             trackSourceInfo);
        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", event.timestamp())
                                               .adUrn(audioAd.getCompanionAdUrn().get().toString())
                                               .pageName(Screen.LIKES.get())
                                               .impressionName("companion_display")
                                               .monetizationType("audio_ad")
                                               .monetizedObject(audioAd.getMonetizableTrackUrn().toString())
                                               .externalMedia(audioAd.getCompanionImageUrl().get().toString()));
    }

    @Test
    public void createsPlaybackPerformanceJsonForPlayEvent() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(PlaybackType.AUDIO_DEFAULT)
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HTTPS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost(CDN_URL)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(LOGGED_IN_USER)
                                                                 .details(DETAILS_JSON)
                                                                 .build();

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "play"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer()
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HTTPS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost(CDN_URL)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(LOGGED_IN_USER)
                                                                 .details(DETAILS_JSON)
                                                                 .build();

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "buffer"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist()
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HTTPS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost(CDN_URL)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(LOGGED_IN_USER)
                                                                 .details(DETAILS_JSON)
                                                                 .build();

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "playlist"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForSeekEvent() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek()
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HTTPS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost(CDN_URL)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(LOGGED_IN_USER)
                                                                 .details(DETAILS_JSON)
                                                                 .build();

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "seek"));
    }

    @Test
    public void createsPlaybackPerformanceJsonForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate()
                                                                 .metricValue(1000L)
                                                                 .protocol(PlaybackProtocol.HTTPS)
                                                                 .playerType(PlayerType.MEDIA_PLAYER)
                                                                 .connectionType(ConnectionType.FOUR_G)
                                                                 .cdnHost(CDN_URL)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(LOGGED_IN_USER)
                                                                 .details(DETAILS_JSON)
                                                                 .build();

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "fragmentRate"));
    }

    @Test
    public void createsPlaybackErrorJsonForErrorEvent() throws Exception {
        final String userAgent = "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)";
        when(deviceHelper.getUserAgent()).thenReturn(userAgent);
        PlaybackErrorEvent event = new PlaybackErrorEvent("category",
                                                          PlaybackProtocol.HTTPS,
                                                          "cdn-uri",
                                                          MEDIA_TYPE,
                                                          BIT_RATE,
                                                          ConnectionType.FOUR_G);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("audio_error", "v0.0.0", event.getTimestamp())
                                               .protocol("https")
                                               .connectionType("4g")
                                               .os(userAgent)
                                               .bitrate("128000")
                                               .format("mp3")
                                               .errorCode("category")
                                               .url("cdn-uri"));
    }

    @Test
    public void createsPromotedTrackClickJson() throws Exception {
        TrackItem item = PlayableFixtures.expectedPromotedTrack();
        PromotedTrackingEvent click = PromotedTrackingEvent.forPromoterClick(item, "stream");

        jsonDataBuilder.build(click);

        String promotedBy = item.promoterUrn().get().toString();
        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", click.getTimestamp())
                                               .pageName("stream")
                                               .monetizationType("promoted")
                                               .adUrn(item.adUrn())
                                               .promotedBy(promotedBy)
                                               .clickObject(item.getUrn().toString())
                                               .clickTarget(promotedBy)
                                               .clickName("item_navigation"));
    }

    @Test
    public void createsPromotedTrackImpressionJson() throws Exception {
        TrackItem item = PlayableFixtures.expectedPromotedTrack();
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(item, "stream");

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                                               .pageName("stream")
                                               .monetizationType("promoted")
                                               .adUrn(item.adUrn())
                                               .promotedBy(item.promoterUrn().get().toString())
                                               .impressionName("promoted_track")
                                               .impressionObject(item.getUrn().toString()));
    }

    @Test
    public void createsPromotedPlaylistImpressionJson() throws Exception {
        PlaylistItem item = PlayableFixtures.expectedPromotedPlaylist();
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(item, "stream");

        jsonDataBuilder.build(impression);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", impression.getTimestamp())
                                               .pageName("stream")
                                               .monetizationType("promoted")
                                               .adUrn(item.adUrn())
                                               .promotedBy(item.promoterUrn().get().toString())
                                               .impressionName("promoted_playlist")
                                               .impressionObject(item.getUrn().toString()));
    }

    private EventLoggerEventData getPlaybackPerformanceEventFor(PlaybackPerformanceEvent event, String type) {
        return getEventData("audio_performance", "v0.0.0", event.timestamp())
                .latency(1000L)
                .protocol("https")
                .playerType("MediaPlayer")
                .connectionType("4g")
                .type(type)
                .format(MEDIA_TYPE)
                .bitrate(BIT_RATE)
                .host(CDN_URL)
                .playbackDetails(DETAILS_JSON);
    }

    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, long timestamp) {
        return new EventLoggerEventData(eventName, boogalooVersion, 3152, UDID, LOGGED_IN_USER.toString(), timestamp);
    }

}
