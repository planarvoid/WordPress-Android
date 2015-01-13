package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.events.ConnectionType;

import com.soundcloud.android.events.PlayerType;
import static com.soundcloud.android.matchers.SoundCloudMatchers.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerUrlBuilderTest {

    private static final String APP_ID = "123";
    private static final String CDN_URL = "host.com";
    private static final String PROTOCOL = "hls";
    private static final PropertySet TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(123L));
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "3g";

    @Mock private Resources resources;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private DeviceHelper deviceHelper;

    private Urn userUrn = Urn.forUser(123L);
    private EventLoggerUrlBuilder eventLoggerUrlBuilder;

    @Before
    public void setUp() throws Exception {
        when(resources.getString(R.string.app_id)).thenReturn(APP_ID);
        when(resources.getString(R.string.event_logger_base_url)).thenReturn("http://eventlogger.soundcloud.com");
        when(trackSourceInfo.getOriginScreen()).thenReturn("origin");
        when(trackSourceInfo.getIsUserTriggered()).thenReturn(true);
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        when(deviceHelper.getUDID()).thenReturn("9876");

        eventLoggerUrlBuilder = new EventLoggerUrlBuilder(resources, experimentOperations, deviceHelper);
    }

    @Test
    public void createAudioEventUrlWithOriginAndTrigger() throws Exception {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createPlayEvent(userUrn, trackSourceInfo, 321L));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&action=play"
                        + "&ts=321"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud%3Asounds%3A123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&policy=allow"
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
        )));
    }

    @Test
    public void createAudioEventUrlWithSourceAndSourceVersion() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createPlayEvent(userUrn, trackSourceInfo, 321L));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&duration=1000"
                + "&protocol=hls"
                + "&ts=321"
                + "&action=play"
                + "&sound=soundcloud:sounds:123"
                + "&user=" + userUrn.toEncodedString()
                + "&trigger=manual"
                + "&page_name=origin"
                + "&policy=allow"
                + "&source=source1"
                + "&source_version=version1"
                + "&player_type=PLAYA"
                + "&connection_type=3g"
        )));
    }

    @Test
    public void createAudioEventUrlFromPlaylist() throws Exception {
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistUrn()).thenReturn(Urn.forPlaylist(123L));
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createPlayEvent(userUrn, trackSourceInfo, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&action=play"
                + "&duration=1000"
                + "&protocol=hls"
                + "&sound=soundcloud:sounds:123"
                + "&user=" + userUrn.toEncodedString()
                + "&trigger=manual"
                + "&page_name=origin"
                + "&policy=allow"
                + "&set_id=123"
                + "&set_position=2"
                + "&player_type=PLAYA"
                + "&connection_type=3g"
        )));
    }

    @Test
    public void createAudioEventUrlForExperimentAssignment() throws Exception {
        Map<String, Integer> experimentParams = Maps.newHashMap();
        experimentParams.put("exp_android-ui", 4);
        experimentParams.put("exp_android-listen", 5);
        when(experimentOperations.getTrackingParams()).thenReturn(experimentParams);
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createPlayEvent(userUrn, trackSourceInfo, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&action=play"
                + "&ts=321"
                + "&duration=1000"
                + "&protocol=hls"
                + "&sound=soundcloud:sounds:123"
                + "&user=" + userUrn.toEncodedString()
                + "&trigger=manual"
                + "&page_name=origin"
                + "&policy=allow"
                + "&exp_android-ui=4"
                + "&exp_android-listen=5"
                + "&player_type=PLAYA"
                + "&connection_type=3g"
        )));
    }

    @Test
    public void createFullPlayAudioEventUrl() {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistUrn()).thenReturn(Urn.forPlaylist(123L));
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createPlayEvent(userUrn, trackSourceInfo, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&action=play"
                + "&duration=1000"
                + "&protocol=hls"
                + "&sound=soundcloud:sounds:123"
                + "&user=" + userUrn.toEncodedString()
                + "&trigger=manual"
                + "&page_name=origin"
                + "&source=source1"
                + "&source_version=version1"
                + "&policy=allow"
                + "&set_id=123"
                + "&set_position=2"
                + "&player_type=PLAYA"
                + "&connection_type=3g"
        )));
    }


    @Test
    public void createStopForPauseAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_PAUSE));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=pause"
        )));
    }

    @Test
    public void createStopForBufferingAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_BUFFERING));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=buffering"
        )));
    }

    @Test
    public void createStopForSkipAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_SKIP));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=skip"
        )));
    }

    @Test
    public void createStopForTrackFinishedAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=track_finished"
        )));
    }

    @Test
    public void createStopForEndOfQueueAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=end_of_content"
        )));
    }

    @Test
    public void createStopForNewQueueAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_NEW_QUEUE));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=context_change"
        )));
    }

    @Test
    public void createStopForErrorAudioEventUrl() {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(createDefaultStopEventForReason(PlaybackSessionEvent.STOP_REASON_ERROR));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                        + "client_id=123"
                        + "&anonymous_id=9876"
                        + "&ts=100000"
                        + "&action=stop"
                        + "&duration=1000"
                        + "&protocol=hls"
                        + "&sound=soundcloud:sounds:123"
                        + "&user=" + userUrn.toEncodedString()
                        + "&trigger=manual"
                        + "&page_name=origin"
                        + "&policy=allow"
                        + "&player_type=PLAYA"
                        + "&connection_type=3g"
                        + "&reason=playback_error"
        )));
    }

    PlaybackSessionEvent createDefaultStopEventForReason(int reason) {
        final PlaybackSessionEvent playEvent = createPlayEvent(userUrn, trackSourceInfo, 3000L);
        return createStopEvent(userUrn, trackSourceInfo, playEvent, 100000L, reason);
    }

    private PlaybackSessionEvent createPlayEvent(Urn user, TrackSourceInfo trackSourceInfo, long timestamp) {
        return PlaybackSessionEvent.forPlay(TRACK_DATA, user, trackSourceInfo, 0L, timestamp, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
    }

    private PlaybackSessionEvent createStopEvent(Urn user, TrackSourceInfo trackSourceInfo, PlaybackSessionEvent playEvent, long timestamp, int stopReason) {
        return PlaybackSessionEvent.forStop(TRACK_DATA, user, trackSourceInfo, playEvent, 0L, timestamp, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, stopReason);
    }

        @Test
    public void createAudioEventUrlForAudioAdPlaybackEvent() throws UnsupportedEncodingException {
        final PropertySet audioAdMetadata = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(audioAdTrack, userUrn, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE).withAudioAd(audioAdMetadata));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&action=play"
                + "&ts=321"
                + "&duration=" + audioAdTrack.get(PlayableProperty.DURATION)
                + "&sound=" + "soundcloud:sounds:" + audioAdTrack.get(TrackProperty.URN).getNumericId()
                + "&user=" + userUrn.toEncodedString()
                + "&trigger=manual"
                + "&page_name=origin"
                + "&protocol=hls"
                + "&ad_urn=" + URLEncoder.encode(audioAdMetadata.get(AdProperty.AD_URN), "utf8")
                + "&monetization_type=audio_ad"
                + "&monetized_object=" + audioAdMetadata.get(AdProperty.MONETIZABLE_TRACK_URN).toEncodedString()
                + "&player_type=PLAYA"
                + "&connection_type=3g"
        )));
    }

    @Test
    public void createImpressionUrlForAudioAdPlaybackEvent() throws CreateModelException, UnsupportedEncodingException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final String url = eventLoggerUrlBuilder.buildForAudioAdImpression(
                PlaybackSessionEvent.forPlay(audioAdTrack, userUrn, trackSourceInfo, 0L, 321L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE).withAudioAd(audioAd));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "origin"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), "utf8")
                + "&impression_name=audio_ad_impression"
                + "&impression_object=" + audioAdTrack.get(TrackProperty.URN).toEncodedString()
                + "&monetization_type=audio_ad"
                + "&monetized_object=" + audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toEncodedString())));
    }

    @Test
    public void createImpressionUrlForCompanionDisplayToAudioAd() throws CreateModelException, UnsupportedEncodingException {
        Urn audioAdTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(audioAdTrackUrn)
                .put(AdProperty.ARTWORK, Uri.parse("http://artwork.org/image.pmg?a=b&c=d"));
        final String url = eventLoggerUrlBuilder.build(
                new VisualAdImpressionEvent(audioAd, audioAdTrackUrn, userUrn, trackSourceInfo, 321L));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "origin"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), Charsets.UTF_8.displayName())
                + "&impression_name=companion_display"
                + "&impression_object=" + audioAdTrackUrn.toEncodedString()
                + "&monetization_type=" + "audio_ad"
                + "&monetized_object=" + audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toEncodedString()
                + "&external_media=" + "http%3A%2F%2Fartwork.org%2Fimage.pmg%3Fa%3Db%26c%3Dd")));
    }

    @Test
    public void createUrlForLeaveBehindImpression() throws CreateModelException, UnsupportedEncodingException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet leaveBehind = TestPropertySets.leaveBehindForPlayer();
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(321, leaveBehind, monetizedTrack, userUrn, sourceInfo);
        final String url = eventLoggerUrlBuilder.build(event);

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "page_source"
                + "&impression_name=leave_behind"
                + "&impression_object=" + leaveBehind.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN)
                + "&ad_urn=" + URLEncoder.encode(leaveBehind.get(LeaveBehindProperty.AD_URN), Charsets.UTF_8.displayName())
                + "&monetized_object=" + monetizedTrack.toEncodedString()
                + "&monetization_type=audio_ad"
                + "&external_media=" + leaveBehind.get(LeaveBehindProperty.IMAGE_URL))));
    }

    @Test
    public void createUrlForInterstitialImpression() throws CreateModelException, UnsupportedEncodingException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet interstitial = TestPropertySets.interstitialForPlayer();
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forImpression(321, interstitial, monetizedTrack, userUrn, sourceInfo);
        final String url = eventLoggerUrlBuilder.build(event);

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "page_source"
                + "&impression_name=interstitial"
                + "&impression_object=" + monetizedTrack.toEncodedString()
                + "&ad_urn=" + URLEncoder.encode(interstitial.get(InterstitialProperty.INTERSTITIAL_URN), Charsets.UTF_8.displayName())
                + "&monetized_object=" + monetizedTrack.toEncodedString()
                + "&monetization_type=interstitial"
                + "&external_media=" + interstitial.get(InterstitialProperty.IMAGE_URL))));
    }

    @Test
    public void createUrlForLeaveBehindClick() throws CreateModelException, UnsupportedEncodingException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet leaveBehind = TestPropertySets.leaveBehindForPlayer();
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(321, leaveBehind, monetizedTrack, userUrn, sourceInfo);
        final String url = eventLoggerUrlBuilder.build(event);

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "page_source"
                + "&click_name=" + "clickthrough::leave_behind"
                + "&click_object=" + leaveBehind.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN)
                + "&click_target=" + leaveBehind.get(LeaveBehindProperty.CLICK_THROUGH_URL)
                + "&ad_urn=" + URLEncoder.encode(leaveBehind.get(LeaveBehindProperty.AD_URN), Charsets.UTF_8.displayName())
                + "&monetized_object=" + monetizedTrack.toEncodedString()
                + "&monetization_type=audio_ad"
                + "&external_media=" + leaveBehind.get(LeaveBehindProperty.IMAGE_URL))));
    }

    @Test
    public void createUrlForInterstitialClick() throws CreateModelException, UnsupportedEncodingException {
        final Urn monetizedTrack = Urn.forTrack(123L);
        final PropertySet interstitial = TestPropertySets.interstitialForPlayer();
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(321, interstitial, monetizedTrack, userUrn, sourceInfo);
        final String url = eventLoggerUrlBuilder.build(event);

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "page_source"
                + "&click_name=" + "clickthrough::interstitial"
                + "&click_target=" + interstitial.get(InterstitialProperty.CLICK_THROUGH_URL)
                + "&ad_urn=" + URLEncoder.encode(interstitial.get(InterstitialProperty.INTERSTITIAL_URN), Charsets.UTF_8.displayName())
                + "&monetized_object=" + monetizedTrack.toEncodedString()
                + "&monetization_type=interstitial"
                + "&external_media=" + interstitial.get(InterstitialProperty.IMAGE_URL))));
    }

    @Test
    public void createAudioAdCompanionDisplayClickUrl() throws UnsupportedEncodingException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);
        final String url = eventLoggerUrlBuilder.build(UIEvent.fromAudioAdCompanionDisplayClick(audioAd, audioAdTrackUrn, userUrn, trackSourceInfo, 1000L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&user=" + userUrn.toEncodedString()
                + "&anonymous_id=9876"
                + "&ts=1000"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), "utf8")
                + "&page_name=" + "origin"
                + "&click_name=clickthrough::companion_display"
                + "&click_object=" + audioAdTrackUrn.toEncodedString()
                + "&click_target=" + audioAd.get(AdProperty.CLICK_THROUGH_LINK)
                + "&external_media=" + audioAd.get(AdProperty.ARTWORK)
                + "&monetized_object=" + monetizedTrackUrn.toEncodedString()
                + "&monetization_type=audio_ad")));
    }

    @Test
    public void createAudioAdSkippedClickUrl() throws UnsupportedEncodingException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);
        final String url = eventLoggerUrlBuilder.build(UIEvent.fromSkipAudioAdClick(audioAd, audioAdTrackUrn, userUrn, trackSourceInfo, 1000L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=1000"
                + "&user=" + userUrn.toEncodedString()
                + "&page_name=" + "origin"
                + "&click_name=ad::skip"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), "utf8")
                + "&click_object=" + audioAdTrackUrn.toEncodedString()
                + "&external_media=" + audioAd.get(AdProperty.ARTWORK)
                + "&monetized_object=" + monetizedTrackUrn.toEncodedString()
                + "&monetization_type=audio_ad")));
    }

    // althought technically an audio event, we currently track this as a click:
    // https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/audio-ads-tracking.md#clicks
    @Test
    public void createAudioAdFinishedClickUrl() throws UnsupportedEncodingException, CreateModelException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final PlaybackSessionEvent stopEvent = TestEvents.playbackSessionStopEvent();
        final String url = eventLoggerUrlBuilder.buildForAdFinished(stopEvent.withAudioAd(audioAd));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=1000"
                + "&user=" + "soundcloud:users:1"
                + "&page_name=" + "screen"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), "utf8")
                + "&click_name=ad::finish"
                + "&click_object=" + URLEncoder.encode(stopEvent.get(PlaybackSessionEvent.KEY_TRACK_URN), "utf8")
                + "&external_media=" + audioAd.get(AdProperty.ARTWORK)
                + "&monetized_object=" + monetizedTrackUrn.toEncodedString()
                + "&monetization_type=audio_ad")));
    }

    @Test
    public void createsPlaybackPerformanceUrlForPlayEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent =
                PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.build(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&latency=1000&protocol=https&player_type=MediaPlayer&type=play&host=host.com&user=" + userUrn.toEncodedString() + "&connection_type=4g&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForBufferEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToBuffer(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        String url = eventLoggerUrlBuilder.build(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=buffer&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToPlaylist(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.build(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=playlist&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForSeekEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToSeek(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.build(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=seek&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.build(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=fragmentRate&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackErrorUrlForErrorEvent() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        PlaybackErrorEvent playbackErrorEvent = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3);
        final String url = eventLoggerUrlBuilder.build(playbackErrorEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_error?client_id=123&anonymous_id=9876&protocol=https&os=SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)&bitrate=128&format=mp3&url=cdn-uri&errorCode=category&ts=" + playbackErrorEvent.getTimestamp())));
    }

}
