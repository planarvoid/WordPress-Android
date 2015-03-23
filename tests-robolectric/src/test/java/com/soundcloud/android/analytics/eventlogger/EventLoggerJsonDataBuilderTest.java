package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerJsonDataBuilderTest {

    private static final Urn LOGGED_IN_USER = Urn.forUser(123L);
    private static final String UDID = "udid";
    private static final long TIMESTAMP = new Date().getTime();
    private static final TrackSourceInfo TRACK_SOURCE_INFO = new TrackSourceInfo(Screen.SIDE_MENU_LIKES.get(), true);
    private static final String PROTOCOL = "hls";
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";
    private static final String CDN_URL = "host.com";

    @Mock private DeviceHelper deviceHelper;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private JsonTransformer jsonTransformer;

    private EventLoggerJsonDataBuilder jsonDataBuilder;

    @Before
    public void setUp() throws Exception {
        jsonDataBuilder = new EventLoggerJsonDataBuilder(Robolectric.application.getResources(), experimentOperations,
                deviceHelper, accountOperations, jsonTransformer);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER);
        when(deviceHelper.getUDID()).thenReturn(UDID);
    }

    @Test
    public void createsScreenEventJson() throws ApiMapperException {
        ScreenEvent screenEvent = ScreenEvent.create(Screen.ACTIVITIES);

        jsonDataBuilder.build(screenEvent);

        String timestamp = String.valueOf(screenEvent.getTimeStamp());
        verify(jsonTransformer).toJson(eq(getEventData("pageview", "v0.0.0", timestamp).pageName(Screen.ACTIVITIES.get())));
    }

    @Test
    public void createAudioAdCompanionDisplayClickEventJson() throws ApiMapperException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);

        jsonDataBuilder.build(UIEvent.fromAudioAdCompanionDisplayClick(audioAd, audioAdTrackUrn, LOGGED_IN_USER, TRACK_SOURCE_INFO, TIMESTAMP));

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", String.valueOf(TIMESTAMP))
            .adUrn(audioAd.get(AdProperty.AD_AUDIO_URN))
            .pageName(Screen.SIDE_MENU_LIKES.get())
            .clickName("clickthrough::companion_display")
            .clickTarget(audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString())
            .clickObject(audioAdTrackUrn.toString())
            .externalMedia(audioAd.get(AdProperty.ARTWORK).toString())
            .monetizedObject(monetizedTrackUrn.toString())
            .monetizationType("audio_ad"));
    }

    @Test
    public void createAudioAdSkippedClickEventJson() throws ApiMapperException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);

        jsonDataBuilder.build(UIEvent.fromSkipAudioAdClick(audioAd, audioAdTrackUrn, LOGGED_IN_USER, TRACK_SOURCE_INFO, TIMESTAMP));

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", String.valueOf(TIMESTAMP))
                .adUrn(audioAd.get(AdProperty.AD_AUDIO_URN))
                .pageName(Screen.SIDE_MENU_LIKES.get())
                .clickName("ad::skip")
                .clickObject(audioAdTrackUrn.toString())
                .externalMedia(audioAd.get(AdProperty.ARTWORK).toString())
                .monetizedObject(monetizedTrackUrn.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createJsonForLeaveBehindImpression() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet leaveBehind = TestPropertySets.leaveBehindForPlayer();

        jsonDataBuilder.build(AdOverlayTrackingEvent.forImpression(TIMESTAMP, leaveBehind, monetizedTrack, LOGGED_IN_USER, TRACK_SOURCE_INFO));

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", String.valueOf(TIMESTAMP))
                .adUrn(leaveBehind.get(LeaveBehindProperty.AD_URN))
                .pageName(Screen.SIDE_MENU_LIKES.get())
                .impressionName("leave_behind")
                .impressionObject(leaveBehind.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN).toString())
                .externalMedia(leaveBehind.get(LeaveBehindProperty.IMAGE_URL))
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createJsonForInterstitialImpression() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet interstitial = TestPropertySets.interstitialForPlayer();
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(TIMESTAMP, interstitial, monetizedTrack, LOGGED_IN_USER, TRACK_SOURCE_INFO);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", String.valueOf(TIMESTAMP))
                .adUrn(interstitial.get(InterstitialProperty.INTERSTITIAL_URN))
                .pageName(Screen.SIDE_MENU_LIKES.get())
                .impressionName("interstitial")
                .impressionObject(monetizedTrack.toString())
                .externalMedia(interstitial.get(InterstitialProperty.IMAGE_URL))
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("interstitial"));
    }

    @Test
    public void createJsonForLeaveBehindClick() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet leaveBehind = TestPropertySets.leaveBehindForPlayer();
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(TIMESTAMP, leaveBehind, monetizedTrack, LOGGED_IN_USER, TRACK_SOURCE_INFO);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", String.valueOf(TIMESTAMP))
                .adUrn(leaveBehind.get(LeaveBehindProperty.AD_URN))
                .pageName(Screen.SIDE_MENU_LIKES.get())
                .clickName("clickthrough::leave_behind")
                .clickObject(leaveBehind.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN).toString())
                .clickTarget(String.valueOf(leaveBehind.get(LeaveBehindProperty.CLICK_THROUGH_URL)))
                .externalMedia(leaveBehind.get(LeaveBehindProperty.IMAGE_URL))
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createJsonForInterstitialClick() throws ApiMapperException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet interstitial = TestPropertySets.interstitialForPlayer();
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(TIMESTAMP, interstitial, monetizedTrack, LOGGED_IN_USER, TRACK_SOURCE_INFO);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", String.valueOf(TIMESTAMP))
                .adUrn(interstitial.get(InterstitialProperty.INTERSTITIAL_URN))
                .pageName(Screen.SIDE_MENU_LIKES.get())
                .clickName("clickthrough::interstitial")
                .clickTarget(String.valueOf(interstitial.get(InterstitialProperty.CLICK_THROUGH_URL)))
                .monetizedObject(monetizedTrack.toString())
                .monetizationType("interstitial")
                .externalMedia(interstitial.get(InterstitialProperty.IMAGE_URL)));
    }


    @Test
    public void createImpressionJsonForCompanionDisplayToAudioAd() throws ApiMapperException {
        Urn audioAdTrackUrn = Urn.forTrack(123L);
        final String artworkUrl = "http://artwork.org/image.pmg?a=b&c=d";
        final PropertySet audioAd = TestPropertySets.audioAdProperties(audioAdTrackUrn)
                .put(AdProperty.ARTWORK, Uri.parse(artworkUrl));

        jsonDataBuilder.build(new VisualAdImpressionEvent(audioAd, audioAdTrackUrn, LOGGED_IN_USER, TRACK_SOURCE_INFO, TIMESTAMP));

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", String.valueOf(TIMESTAMP))
                .adUrn(audioAd.get(AdProperty.AD_AUDIO_URN))
                .pageName(Screen.SIDE_MENU_LIKES.get())
                .impressionName("companion_display")
                .impressionObject(audioAdTrackUrn.toString())
                .monetizationType("audio_ad")
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .externalMedia(artworkUrl));
    }

    @Test
    public void createAudioAdFinishedEventJson() throws ApiMapperException, CreateModelException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final PlaybackSessionEvent stopEvent = TestEvents.playbackSessionStopEvent();

        jsonDataBuilder.buildForAdFinished(stopEvent.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("click", "v0.0.0", String.valueOf(stopEvent.getTimeStamp()))
                .pageName(stopEvent.getTrackSourceInfo().getOriginScreen())
                .adUrn(audioAd.get(AdProperty.AD_AUDIO_URN))
                .clickName("ad::finish")
                .clickObject(stopEvent.get(PlaybackSessionEvent.KEY_TRACK_URN))
                .monetizedObject(monetizedTrackUrn.toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createImpressionJsonForAudioAdPlaybackEvent() throws ApiMapperException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, TRACK_SOURCE_INFO, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);

        jsonDataBuilder.buildForAudioAdImpression(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("impression", "v0.0.0", String.valueOf(event.getTimeStamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .adUrn(audioAd.get(AdProperty.AD_AUDIO_URN))
                .impressionName("audio_ad_impression")
                .impressionObject(audioAdTrack.get(TrackProperty.URN).toString())
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createAudioEventUrlForAudioAdPlaybackEvent() throws ApiMapperException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(audioAdTrack, LOGGED_IN_USER, TRACK_SOURCE_INFO, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);

        TRACK_SOURCE_INFO.setSource("source", "source-version");
        TRACK_SOURCE_INFO.setOriginPlaylist(Urn.forPlaylist(123L), 2, Urn.forUser(321L));

        jsonDataBuilder.buildForAudioEvent(event.withAudioAd(audioAd));

        verify(jsonTransformer).toJson(getEventData("audio", "v0.0.0", String.valueOf(event.getTimeStamp()))
                .pageName(event.getTrackSourceInfo().getOriginScreen())
                .duration(audioAdTrack.get(PlayableProperty.DURATION))
                .sound("soundcloud:sounds:" + audioAdTrack.get(TrackProperty.URN).getNumericId())
                .trigger("manual")
                .source("source")
                .sourceVersion("source-version")
                .playlistId("123")
                .playlistPosition("2")
                .protocol("hls")
                .playerType("PLAYA")
                .connectionType("3g")
                .adUrn(audioAd.get(AdProperty.AD_AUDIO_URN))
                .monetizedObject(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString())
                .monetizationType("audio_ad"));
    }

    @Test
    public void createsPlaybackPerformanceUrlForPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "play"));
    }

    @Test
    public void createsPlaybackPerformanceUrlForBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "buffer"));
    }

    @Test
    public void createsPlaybackPerformanceUrlForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "playlist"));
    }

    @Test
    public void createsPlaybackPerformanceUrlForSeekEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "seek"));
    }

    @Test
    public void createsPlaybackPerformanceUrlForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, LOGGED_IN_USER);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getPlaybackPerformanceEventFor(event, "fragmentRate"));
    }

    @Test
    public void createsPlaybackErrorUrlForErrorEvent() throws Exception {
        final String userAgent = "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)";
        when(deviceHelper.getUserAgent()).thenReturn(userAgent);
        PlaybackErrorEvent event = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3, ConnectionType.FOUR_G);

        jsonDataBuilder.build(event);

        verify(jsonTransformer).toJson(getEventData("audio_error", "v0.0.0", String.valueOf(event.getTimestamp()))
                .protocol("https")
                .connectionType("4g")
                .os(userAgent)
                .bitrate("128")
                .format("mp3")
                .errorCode("category")
                .url("cdn-uri"));
    }

    private EventLoggerEventData getPlaybackPerformanceEventFor(PlaybackPerformanceEvent event, String type) {
        return getEventData("audio_performance", "v0.0.0", String.valueOf(event.getTimeStamp()))
                .latency(1000L)
                .protocol("https")
                .playerType("MediaPlayer")
                .connectionType("4g")
                .type(type)
                .host("host.com");
    }

    private EventLoggerEventData getEventData(String eventName, String boogalooVersion, String timestamp) {
        return new EventLoggerEventData(eventName, boogalooVersion, "3152", UDID, LOGGED_IN_USER.toString(), timestamp);
    }

}
