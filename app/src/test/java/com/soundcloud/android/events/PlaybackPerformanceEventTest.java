package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.playback.PlaybackProtocol;
import org.junit.Test;

public class PlaybackPerformanceEventTest {

    private static final String CDN_HOST = "ec-media.com";
    private static final String PLAYER_TYPE = "fakeTestPlayerType";
    private static final String PROTOCOL = PlaybackProtocol.HLS.getValue();
    private static final String MEDIA_TYPE = "mp3";
    private static final int BIT_RATE = 128000;

    @Test
    public void createTimeToPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
    }

    @Test
    public void createTimeToPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
    }

    @Test
    public void createTimeToBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
    }

    @Test
    public void createFragmentDownloadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
    }

    @Test
    public void createTimeToSeekEvent() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
    }

    @Test
    public void createTimeToLoadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToLoad()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
    }

    @Test
    public void createUninterruptedPlaytimeEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs()
                                                                 .metricValue(1000L)
                                                                 .playbackProtocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
    }
}
