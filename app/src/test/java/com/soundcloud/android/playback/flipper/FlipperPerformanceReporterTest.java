package com.soundcloud.android.playback.flipper;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPerformanceEvent;
import com.soundcloud.android.playback.PlaybackMetric;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlipperPerformanceReporterTest {

    private static final PlaybackProtocol PROTOCOL = PlaybackProtocol.HLS;
    private static final PlayerType PLAYER_TYPE = PlayerType.FLIPPER;
    private static final Urn USER_URN = Urn.forUser(1234567L);
    private static final Long LATENCY = 1000L;
    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private static final String FORMAT = "opus";
    private static final int BITRATE = 128000;
    private static final ConnectionType CONNECTION_TYPE = ConnectionType.FOUR_G;

    private TestEventBusV2 eventBus = new TestEventBusV2();

    private FlipperPerformanceReporter performanceReporter;

    @Before
    public void setUp() {
        performanceReporter = new FlipperPerformanceReporter(eventBus);
    }

    @Test
    public void doesNotReportAudioAdPerformance() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_PLAY);
        performanceReporter.report(PlaybackType.AUDIO_AD, audioPerformanceEvent, PlayerType.FLIPPER, USER_URN, CONNECTION_TYPE);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    @Test
    public void doesNotReportVideoAdPerformance() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_PLAY);
        performanceReporter.report(PlaybackType.VIDEO_AD, audioPerformanceEvent, PlayerType.FLIPPER, USER_URN, CONNECTION_TYPE);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    @Test
    public void reportsTimeToPlayEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_PLAY);

        performanceReporter.report(PlaybackType.AUDIO_DEFAULT, audioPerformanceEvent, PLAYER_TYPE, USER_URN, CONNECTION_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
    }

    @Test
    public void reportsTimeToSeekEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_SEEK);

        performanceReporter.report(PlaybackType.AUDIO_DEFAULT, audioPerformanceEvent, PLAYER_TYPE, USER_URN, CONNECTION_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
    }

    @Test
    public void reportsTimeToPlaylistEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_GET_PLAYLIST);

        performanceReporter.report(PlaybackType.AUDIO_DEFAULT, audioPerformanceEvent, PLAYER_TYPE, USER_URN, CONNECTION_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
    }

    @Test
    public void reportsCacheUsagePercentage() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.CACHE_USAGE_PERCENT);

        performanceReporter.report(PlaybackType.AUDIO_DEFAULT, audioPerformanceEvent, PLAYER_TYPE, USER_URN, CONNECTION_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_CACHE_USAGE_PERCENT);
    }

    private AudioPerformanceEvent createAudioPerformanceEventWithType(PlaybackMetric metric) {
        return new AudioPerformanceEvent(metric, LATENCY, PROTOCOL.getValue(), CDN_HOST, FORMAT, BITRATE, null);
    }

    private void assertPerformanceEvent(PlaybackPerformanceEvent event, int metric) {
        assertThat(event.metric()).isEqualTo(metric);
        assertThat(event.metricValue()).isEqualTo(LATENCY);
        assertThat(event.protocol()).isEqualTo(PROTOCOL);
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE);
        assertThat(event.connectionType()).isEqualTo(CONNECTION_TYPE);
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.format()).isEqualTo(FORMAT);
        assertThat(event.bitrate()).isEqualTo(BITRATE);
        assertThat(event.userUrn()).isEqualTo(USER_URN);
    }
}
