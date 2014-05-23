package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;

import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;

public class PlaybackPerformanceEventTest {

    private static final String CDN_HOST = "ec-media.com";
    private static final PlayerType PLAYER_TYPE = PlayerType.MEDIA_PLAYER;
    private static final PlaybackProtocol PROTOCOL = PlaybackProtocol.HLS;
    private static final ConnectionType CONNECTION_TYPE = ConnectionType.FOUR_G;
    private UserUrn userUrn;
    
    @Before
    public void setUp() throws Exception {
        userUrn = TestHelper.getModelFactory().createModel(UserUrn.class);

    }
    @Test
    public void createTimeToPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
        expect(event.getConnectionType()).toEqual(ConnectionType.FOUR_G);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void createTimeToPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
        expect(event.getConnectionType()).toEqual(ConnectionType.FOUR_G);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void createTimeToBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
        expect(event.getConnectionType()).toEqual(ConnectionType.FOUR_G);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void createFragmentDownloadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
        expect(event.getConnectionType()).toEqual(ConnectionType.FOUR_G);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void createTimeToSeekEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PLAYER_TYPE);
        expect(event.getProtocol()).toEqual(PROTOCOL);
        expect(event.getConnectionType()).toEqual(ConnectionType.FOUR_G);
        expect(event.getUserUrn()).toEqual(userUrn);
    }
}
