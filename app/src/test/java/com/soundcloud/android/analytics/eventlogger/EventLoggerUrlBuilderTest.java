package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.matchers.SoundCloudMatchers.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.LeaveBehindImpressionEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
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

    @Mock private Resources resources;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private DeviceHelper deviceHelper;

    private Urn userUrn = Urn.forUser(123L);
    private EventLoggerUrlBuilder eventLoggerUrlBuilder;

    @Before
    public void setUp() throws Exception {
        when(resources.getString(R.string.app_id)).thenReturn(APP_ID);
        when(trackSourceInfo.getOriginScreen()).thenReturn("origin");
        when(trackSourceInfo.getIsUserTriggered()).thenReturn(true);
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        when(deviceHelper.getUniqueDeviceID()).thenReturn("9876");

        eventLoggerUrlBuilder = new EventLoggerUrlBuilder(resources, experimentOperations, deviceHelper);
    }

    @Test
    public void createAudioEventUrlWithOriginAndTrigger() throws Exception {
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L));
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
                + "&page_name=origin")));
    }

    @Test
    public void createAudioEventUrlWithSourceAndSourceVersion() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L));
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
                + "&source_version=version1")));
    }

    @Test
    public void createAudioEventUrlFromPlaylist() throws Exception {
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistUrn()).thenReturn(Urn.forPlaylist(123L));
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L));
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
                + "&set_position=2")));
    }

    @Test
    public void createAudioEventUrlForExperimentAssignment() throws Exception {
        Map<String, Integer> experimentParams = Maps.newHashMap();
        experimentParams.put("exp_android-ui", 4);
        experimentParams.put("exp_android-listen", 5);
        when(experimentOperations.getTrackingParams()).thenReturn(experimentParams);
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L));
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
                + "&exp_android-listen=5")));
    }

    @Test
    public void createFullAudioEventUrl() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistUrn()).thenReturn(Urn.forPlaylist(123L));
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L));
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
                + "&set_position=2")));
    }

    @Test
    public void createAudioEventUrlForAudioAdPlaybackEvent() throws UnsupportedEncodingException {
        final PropertySet audioAdMetadata = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final String url = eventLoggerUrlBuilder.buildForAudioEvent(
                PlaybackSessionEvent.forPlay(audioAdTrack, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L).withAudioAd(audioAdMetadata));
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
                + "&monetized_object=" + audioAdMetadata.get(AdProperty.MONETIZABLE_TRACK_URN).toEncodedString())));
    }

    @Test
    public void createImpressionUrlForAudioAdPlaybackEvent() throws CreateModelException, UnsupportedEncodingException {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        final PropertySet audioAdTrack = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(456L));
        final String url = eventLoggerUrlBuilder.buildForAudioAdImpression(
                PlaybackSessionEvent.forPlay(audioAdTrack, userUrn, PROTOCOL, trackSourceInfo, 0L, 321L).withAudioAd(audioAd));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
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
        final String url = eventLoggerUrlBuilder.buildForVisualAdImpression(
                new VisualAdImpressionEvent(audioAd, audioAdTrackUrn, userUrn, 321L));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), Charsets.UTF_8.displayName())
                + "&impression_name=companion_display"
                + "&impression_object=" + audioAdTrackUrn.toEncodedString()
                + "&monetization_type=audio_ad"
                + "&monetized_object=" + audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toEncodedString()
                + "&external_media=" + "http%3A%2F%2Fartwork.org%2Fimage.pmg%3Fa%3Db%26c%3Dd")));
    }

    @Test
    public void createImpressionUrlForLeaveBehindDisplayToAudioAd() throws CreateModelException, UnsupportedEncodingException {
        Urn audioAdTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(audioAdTrackUrn)
                .put(AdProperty.ARTWORK, Uri.parse("http://artwork.org/image.pmg?a=b&c=d"));
        final String url = eventLoggerUrlBuilder.buildForLeaveBehindImpression(
                new LeaveBehindImpressionEvent(audioAd, audioAdTrackUrn, userUrn, 321L));

        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=321"
                + "&user=" + userUrn.toEncodedString()
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), Charsets.UTF_8.displayName())
                + "&impression_name=leave_behind"
                + "&impression_object=" + audioAdTrackUrn.toEncodedString()
                + "&monetization_type=audio_ad"
                + "&monetized_object=" + audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toEncodedString()
                + "&external_media=" + "http%3A%2F%2Fartwork.org%2Fimage.pmg%3Fa%3Db%26c%3Dd")));
    }

    @Test
    public void createAudioAdCompanionDisplayClickUrl() throws UnsupportedEncodingException {
        final Urn monetizedTrackUrn = Urn.forTrack(123L);
        final PropertySet audioAd = TestPropertySets.audioAdProperties(monetizedTrackUrn);
        final Urn audioAdTrackUrn = Urn.forTrack(456);
        final String url = eventLoggerUrlBuilder.buildForClick(UIEvent.fromAudioAdCompanionDisplayClick(audioAd, audioAdTrackUrn, 1000L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=1000"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), "utf8")
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
        final String url = eventLoggerUrlBuilder.buildForClick(UIEvent.fromSkipAudioAdClick(audioAd, audioAdTrackUrn, 1000L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/click?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&ts=1000"
                + "&ad_urn=" + URLEncoder.encode(audioAd.get(AdProperty.AD_URN), "utf8")
                + "&click_name=ad::skip"
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
        final String url = eventLoggerUrlBuilder.buildForAudioPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&latency=1000&protocol=https&player_type=MediaPlayer&type=play&host=host.com&user=" + userUrn.toEncodedString() + "&connection_type=4g&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForBufferEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToBuffer(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        String url = eventLoggerUrlBuilder.buildForAudioPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=buffer&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToPlaylist(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildForAudioPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=playlist&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForSeekEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToSeek(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildForAudioPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=seek&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildForAudioPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=fragmentRate&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackErrorUrlForErrorEvent() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        PlaybackErrorEvent playbackErrorEvent = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3);
        final String url = eventLoggerUrlBuilder.buildForAudioErrorEvent(playbackErrorEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_error?client_id=123&anonymous_id=9876&protocol=https&os=SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)&bitrate=128&format=mp3&url=cdn-uri&errorCode=category&ts=" + playbackErrorEvent.getTimestamp())));
    }

}
