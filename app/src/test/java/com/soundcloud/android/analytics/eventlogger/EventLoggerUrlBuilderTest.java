package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.matchers.SoundCloudMatchers.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.PropertySets;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.net.Uri;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerUrlBuilderTest {

    private static final String APP_ID = "123";
    private static final String USER_AGENT_UNENCODED = "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)";
    private static final String CDN_URL = "host.com";
    private static final PropertySet TRACK_DATA = PropertySets.expectedTrackDataForAnalytics(Urn.forTrack(123L));

    @Mock private Resources resources;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private DeviceHelper deviceHelper;

    private UserUrn userUrn = Urn.forUser(123L);
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
        final String url = eventLoggerUrlBuilder.buildFromPlaybackEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, trackSourceInfo, 0L, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?client_id=123&anonymous_id=9876&action=play&ts=321&duration=1000&sound=soundcloud%3Asounds%3A123&user=" + userUrn.toEncodedString() + "&trigger=manual&context=origin")));
    }

    @Test
    public void createAudioEventUrlWithSourceAndSourceVersion() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        final String url = eventLoggerUrlBuilder.buildFromPlaybackEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, trackSourceInfo, 0L, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?client_id=123&anonymous_id=9876&duration=1000&ts=321&action=play&sound=soundcloud:sounds:123&user=" + userUrn.toEncodedString() + "&trigger=manual&context=origin&source=source1&source_version=version1")));
    }

    @Test
    public void createAudioEventUrlFromPlaylist() throws Exception {
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, trackSourceInfo, 0L, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?client_id=123&anonymous_id=9876&ts=321&action=play&duration=1000&sound=soundcloud:sounds:123&user=" + userUrn.toEncodedString() + "&trigger=manual&context=origin&set_id=123&set_position=2")));
    }

    @Test
    public void createAudioEventUrlForExperimentAssignment() throws Exception {
        Map<String, Integer> experimentParams = Maps.newHashMap();
        experimentParams.put("exp_android-ui", 4);
        experimentParams.put("exp_android-listen", 5);
        when(experimentOperations.getTrackingParams()).thenReturn(experimentParams);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, trackSourceInfo, 0L, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?client_id=123&anonymous_id=9876&action=play&ts=321&duration=1000&sound=soundcloud:sounds:123&user=" + userUrn.toEncodedString() + "&trigger=manual&context=origin&exp_android-ui=4&exp_android-listen=5")));
    }

    @Test
    public void createFullAudioEventUrl() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackEvent(
                PlaybackSessionEvent.forPlay(TRACK_DATA, userUrn, trackSourceInfo, 0L, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?client_id=123&anonymous_id=9876&ts=321&action=play&duration=1000&sound=soundcloud:sounds:123&user=" + userUrn.toEncodedString() + "&trigger=manual&context=origin&source=source1&source_version=version1&set_id=123&set_position=2")));
    }

    @Test
    public void createAudioEventUrlForAudioAdPlaybackEvent() throws CreateModelException {
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        TrackUrn monetizedTrack = Urn.forTrack(456L);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackEvent(
                PlaybackSessionEvent.forAdPlay(audioAd, monetizedTrack, userUrn, PlaybackProtocol.HLS, trackSourceInfo, 0L, 321L));
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio?"
                + "client_id=123"
                + "&anonymous_id=9876"
                + "&action=play"
                + "&ts=321"
                + "&duration=12345"
                + "&sound=soundcloud%3Asounds%3A123"
                + "&user=" + userUrn.toEncodedString()
                + "&trigger=manual"
                + "&context=origin"
                + "&protocol=hls"
                + "&ad_urn=adswizz%3Aads%3A123456"
                + "&monetization_type=audio_ad"
                + "&monetized_object=soundcloud%3Asounds%3A456")));
    }

//    @Test
//    public void createImpressionUrlForAudioAdPlaybackEvent() {
//        final String url = eventLoggerUrlBuilder.buildFromAdPlayback(
//                PlaybackSessionEvent.forAdPlay(TRACK_DATA, userUrn, trackSourceInfo, 0L, 321L));
//        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/impression?"
//                + "client_id=123"
//                + "&anonymous_id=9876"
//                + "&ts=321"
//                + "&user=" + userUrn.toEncodedString()
//                + "&ad_urn=adswizz%3Aads%3A123456"
//                + "&impression_name=audio_ad_impression"
//                + "&impression_object=soundcloud%3Asounds%3A123"
//                + "&monetization_type=audio_ad"
//                + "&monetized_object=soundcloud%3Asounds%3A456")));
//    }

    @Test
    public void createsPlaybackPerformanceUrlForPlayEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent =
                PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&latency=1000&protocol=https&player_type=MediaPlayer&type=play&host=host.com&user=" + userUrn.toEncodedString() + "&connection_type=4g&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForBufferEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToBuffer(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        String url = eventLoggerUrlBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=buffer&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToPlaylist(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=playlist&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForSeekEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToSeek(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=seek&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceUrlForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, CDN_URL, userUrn);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?client_id=123&anonymous_id=9876&user=" + userUrn.toEncodedString() + "&protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=fragmentRate&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackErrorUrlForErrorEvent() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        PlaybackErrorEvent playbackErrorEvent = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3);
        final String url = eventLoggerUrlBuilder.buildFromPlaybackErrorEvent(playbackErrorEvent);
        assertThat(url, is(urlEqualTo("http://eventlogger.soundcloud.com/audio_error?client_id=123&anonymous_id=9876&protocol=https&os=SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)&bitrate=128&format=mp3&url=cdn-uri&errorCode=category&ts=" + playbackErrorEvent.getTimestamp())));
    }

    @Test
    public void createsPlaybackErrorUrlForErrorEventEncodesUserAgent() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn(USER_AGENT_UNENCODED);
        PlaybackErrorEvent playbackErrorEvent = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3);
        final String actualUrl = eventLoggerUrlBuilder.buildFromPlaybackErrorEvent(playbackErrorEvent);
        expect(actualUrl.contains(Uri.encode(USER_AGENT_UNENCODED))).toBeTrue();
    }
}
