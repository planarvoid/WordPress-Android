package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.matchers.SoundCloudMatchers.queryStringEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerParamsBuilderTest {

    public static final String USER_AGENT_UNENCODED = "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)";
    @Mock
    private Track track;
    @Mock
    private TrackSourceInfo trackSourceInfo;
    @Mock
    private ExperimentOperations experimentOperations;
    @Mock
    private DeviceHelper deviceHelper;

    private EventLoggerParamsBuilder eventLoggerParamsBuilder;

    @Before
    public void setUp() throws Exception {
        eventLoggerParamsBuilder = new EventLoggerParamsBuilder(experimentOperations, deviceHelper);
        when(trackSourceInfo.getOriginScreen()).thenReturn("origin");
        when(trackSourceInfo.getIsUserTriggered()).thenReturn(true);
    }

    @Test
    public void createParamsWithOriginAndTrigger() throws Exception {
        checkUrl("action=play&ts=321&duration=0&sound=soundcloud:sounds:0&user=soundcloud:users:1&trigger=manual&context=origin");
    }

    @Test
    public void createParamsWithSourceAndSourceVersion() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        checkUrl("duration=0&ts=321&action=play&sound=soundcloud:sounds:0&user=soundcloud:users:1&trigger=manual&context=origin&source=source1&source_version=version1");
    }

    @Test
    public void createParamsFromPlaylist() throws Exception {
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        checkUrl("ts=321&action=play&duration=0&sound=soundcloud:sounds:0&user=soundcloud:users:1&trigger=manual&context=origin&set_id=123&set_position=2");
    }

    @Test
    public void createParamsForExperimentAssignment() throws Exception {
        Map<String, Integer> experimentParams = Maps.newHashMap();
        experimentParams.put("exp_android-ui", 4);
        experimentParams.put("exp_android-listen", 5);
        when(experimentOperations.getTrackingParams()).thenReturn(experimentParams);
        checkUrl("action=play&ts=321&duration=0&sound=soundcloud:sounds:0&user=soundcloud:users:1&trigger=manual&context=origin&exp_android-ui=4&exp_android-listen=5");
    }

    @Test
    public void createFullParams() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        checkUrl("ts=321&action=play&duration=0&sound=soundcloud:sounds:0&user=soundcloud:users:1&trigger=manual&context=origin&source=source1&source_version=version1&set_id=123&set_position=2");
    }

    @Test
    public void createsPlaybackPerformanceParametersForPlayEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToPlay(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, "http://host.com/track.mp3");
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(actualQueryString, is(queryStringEqualTo("protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=play&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceParametersForBufferEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToBuffer(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, "http://host.com/track.mp3");
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(actualQueryString, is(queryStringEqualTo("protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=buffer&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceParametersForPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToPlaylist(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, "http://host.com/track.mp3");
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(actualQueryString, is(queryStringEqualTo("protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=playlist&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceParametersForSeekEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.timeToSeek(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, "http://host.com/track.mp3");
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(actualQueryString, is(queryStringEqualTo("protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=seek&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackPerformanceParametersForFragmentDownloadRateEvent() throws Exception {
        PlaybackPerformanceEvent playbackPerformanceEvent = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.FOUR_G, "http://host.com/track.mp3");
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(playbackPerformanceEvent);
        assertThat(actualQueryString, is(queryStringEqualTo("protocol=https&player_type=MediaPlayer&latency=1000&host=host.com&connection_type=4g&type=fragmentRate&ts=" + playbackPerformanceEvent.getTimeStamp())));
    }

    @Test
    public void createsPlaybackErrorParametersForErrorEvent() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        PlaybackErrorEvent playbackErrorEvent = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3);
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackErrorEvent(playbackErrorEvent);
        assertThat(actualQueryString, is(queryStringEqualTo("protocol=https&os=SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)&bitrate=128&format=mp3&url=cdn-uri&errorCode=category&ts=" + playbackErrorEvent.getTimestamp())));
    }

    @Test
    public void createsPlaybackErrorParametersForErrorEventEncodesUserAgent() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn(USER_AGENT_UNENCODED);
        PlaybackErrorEvent playbackErrorEvent = new PlaybackErrorEvent("category", PlaybackProtocol.HTTPS, "cdn-uri", PlaybackErrorEvent.BITRATE_128, PlaybackErrorEvent.FORMAT_MP3);
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackErrorEvent(playbackErrorEvent);
        expect(actualQueryString.contains(Uri.encode(USER_AGENT_UNENCODED))).toBeTrue();
    }

    private void checkUrl(String expected) throws UnsupportedEncodingException {
        final String actualQueryString = eventLoggerParamsBuilder.buildFromPlaybackEvent(PlaybackEvent.forPlay(track, 1L, trackSourceInfo, 321L));
        assertThat(actualQueryString, is(queryStringEqualTo(expected)));
    }
}
