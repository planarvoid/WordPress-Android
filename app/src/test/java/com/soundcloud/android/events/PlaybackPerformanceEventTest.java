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
    private static final String MEDIA_TYPE = "mp3";
    private static final String VIDEO_MEDIA_TYPE = "video/mp4";
    private static final int BIT_RATE = 128000;

    private Urn userUrn;

    @Before
    public void setUp() throws Exception {
        userUrn = ModelFixtures.create(Urn.class);

    }

    @Test
    public void createTimeToPlayEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L,
                                                                             PROTOCOL,
                                                                             PLAYER_TYPE,
                                                                             CONNECTION_TYPE,
                                                                             CDN_HOST,
                                                                             MEDIA_TYPE,
                                                                             BIT_RATE,
                                                                             userUrn,
                                                                             false);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createTimeToPlayEventForVideo() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(1000L,
                                                                             PROTOCOL,
                                                                             PLAYER_TYPE,
                                                                             CONNECTION_TYPE,
                                                                             CDN_HOST,
                                                                             VIDEO_MEDIA_TYPE,
                                                                             BIT_RATE,
                                                                             userUrn,
                                                                             true);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getFormat()).isEqualTo(VIDEO_MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isTrue();
    }

    @Test
    public void createTimeToPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist(1000L,
                                                                                 PROTOCOL,
                                                                                 PLAYER_TYPE,
                                                                                 CONNECTION_TYPE,
                                                                                 CDN_HOST,
                                                                                 MEDIA_TYPE,
                                                                                 BIT_RATE,
                                                                                 userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getFormat()).isEqualTo(MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createTimeToBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(1000L,
                                                                               PROTOCOL,
                                                                               PLAYER_TYPE,
                                                                               CONNECTION_TYPE,
                                                                               CDN_HOST,
                                                                               MEDIA_TYPE,
                                                                               BIT_RATE,
                                                                               userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getFormat()).isEqualTo(MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createFragmentDownloadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate(1000L,
                                                                                       PROTOCOL,
                                                                                       PLAYER_TYPE,
                                                                                       CONNECTION_TYPE,
                                                                                       CDN_HOST,
                                                                                       MEDIA_TYPE,
                                                                                       BIT_RATE,
                                                                                       userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getFormat()).isEqualTo(MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createTimeToSeekEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek(1000L,
                                                                             PROTOCOL,
                                                                             PLAYER_TYPE,
                                                                             CONNECTION_TYPE,
                                                                             CDN_HOST,
                                                                             MEDIA_TYPE,
                                                                             BIT_RATE,
                                                                             userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getFormat()).isEqualTo(MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createTimeToLoadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToLoad(1000L,
                                                                             PROTOCOL,
                                                                             PLAYER_TYPE,
                                                                             CONNECTION_TYPE,
                                                                             CDN_HOST,
                                                                             MEDIA_TYPE,
                                                                             BIT_RATE,
                                                                             userUrn);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.getFormat()).isEqualTo(MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createUninterruptedPlaytimeEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(1000L,
                                                                                          PROTOCOL,
                                                                                          PLAYER_TYPE,
                                                                                          CONNECTION_TYPE,
                                                                                          CDN_HOST,
                                                                                          MEDIA_TYPE,
                                                                                          BIT_RATE,
                                                                                          false);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(CONNECTION_TYPE);
        assertThat(event.getFormat()).isEqualTo(MEDIA_TYPE);
        assertThat(event.getBitrate()).isEqualTo(BIT_RATE);
        assertThat(event.isVideo()).isFalse();
    }

    @Test
    public void createUninterruptedPlaytimeEventForVideo() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(1000L,
                                                                                          PROTOCOL,
                                                                                          PLAYER_TYPE,
                                                                                          CONNECTION_TYPE,
                                                                                          CDN_HOST,
                                                                                          VIDEO_MEDIA_TYPE,
                                                                                          BIT_RATE,
                                                                                          true);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.getProtocol()).isEqualTo(PROTOCOL);
        assertThat(event.getConnectionType()).isEqualTo(CONNECTION_TYPE);
        assertThat(event.isVideo()).isTrue();
    }
}
