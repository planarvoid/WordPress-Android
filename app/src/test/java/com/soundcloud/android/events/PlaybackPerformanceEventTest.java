package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
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

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(PlaybackType.AUDIO_DEFAULT)
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isVideoAd()).isFalse();
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createTimeToPlayEventForVideoAd() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(PlaybackType.VIDEO_AD)
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(VIDEO_MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.format()).isEqualTo(VIDEO_MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isVideoAd()).isTrue();
        assertThat(event.isAd()).isTrue();
    }

    @Test
    public void createTimeToPlayEventForAudioAd() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(PlaybackType.AUDIO_AD)
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isVideoAd()).isFalse();
        assertThat(event.isAd()).isTrue();
    }

    @Test
    public void createTimeToPlaylistEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlaylist()
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createTimeToBufferEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer()
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createFragmentDownloadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.fragmentDownloadRate()
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createTimeToSeekEvent() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToSeek()
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createTimeToLoadEvent() throws Exception {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToLoad()
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .userUrn(userUrn)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(ConnectionType.FOUR_G);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.userUrn()).isEqualTo(userUrn);
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createUninterruptedPlaytimeEvent() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(PlaybackType.AUDIO_DEFAULT)
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(CONNECTION_TYPE);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.isAd()).isFalse();
    }

    @Test
    public void createUninterruptedPlaytimeEventForVideoAd() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(PlaybackType.VIDEO_AD)
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(VIDEO_MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(CONNECTION_TYPE);
        assertThat(event.isAd()).isTrue();
        assertThat(event.isVideoAd()).isTrue();
    }

    @Test
    public void createUninterruptedPlaytimeEventForAudioAd() throws Exception {

        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(PlaybackType.AUDIO_AD)
                                                                 .metricValue(1000L)
                                                                 .protocol(PROTOCOL)
                                                                 .playerType(PLAYER_TYPE)
                                                                 .connectionType(CONNECTION_TYPE)
                                                                 .cdnHost(CDN_HOST)
                                                                 .format(MEDIA_TYPE)
                                                                 .bitrate(BIT_RATE)
                                                                 .build();
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(1000L);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.connectionType()).isEqualTo(CONNECTION_TYPE);
        assertThat(event.format()).isEqualTo(MEDIA_TYPE);
        assertThat(event.bitrate()).isEqualTo(BIT_RATE);
        assertThat(event.isAd()).isTrue();
        assertThat(event.isVideoAd()).isFalse();
    }
}
