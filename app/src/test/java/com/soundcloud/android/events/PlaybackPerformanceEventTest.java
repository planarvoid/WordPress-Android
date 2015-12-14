package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

public class PlaybackPerformanceEventTest {

    private static final String CDN_HOST = "ec-media.com";
    private static final PlayerType PLAYER_TYPE = PlayerType.MEDIA_PLAYER;
    private static final PlaybackProtocol PROTOCOL = PlaybackProtocol.HLS;
    private static final ConnectionType CONNECTION_TYPE = ConnectionType.FOUR_G;
    private Urn userUrn;
    
    @Before
    public void setUp() throws Exception {
        userUrn = ModelFixtures.create(Urn.class);

    }

    @Test
    public void createTimeToPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void createTimeToPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void createTimeToBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void createFragmentDownloadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void createTimeToSeekEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void createTimeToLoadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToLoad(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST, userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void createUninterruptedPlaytimeEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, CDN_HOST);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(CONNECTION_TYPE);
    }
}
