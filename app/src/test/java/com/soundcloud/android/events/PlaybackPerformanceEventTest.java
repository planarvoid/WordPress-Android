package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.Protocol;

import org.junit.Test;

public class PlaybackPerformanceEventTest {

    public static final String URI = "http://ec-media.com/asdf?p=1";
    public static final PlayerType PLAYER_TYPE = PlayerType.MEDIA_PLAYER;
    public static final Protocol PROTOCOL = Protocol.HLS;

    @Test
    public void createTimeToPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PROTOCOL, PLAYER_TYPE, URI);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(URI);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
    }

    @Test
    public void createTimeToPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist(1000L, PROTOCOL, PLAYER_TYPE, URI);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(URI);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
    }

    @Test
    public void createTimeToBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(1000L, PROTOCOL, PLAYER_TYPE, URI);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(URI);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
    }

    @Test
    public void createFragmentDownloadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PROTOCOL, PLAYER_TYPE, URI);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(URI);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
    }

    @Test
    public void createTimeToSeekEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek(1000L, PROTOCOL, PLAYER_TYPE, URI);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(URI);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
    }
}
