package com.soundcloud.android.playback.skippy;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.playback.AudioPerformanceEvent;
import com.soundcloud.android.playback.PlaybackMetric;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SkippyPerformanceReporterTest {

    private static final PlaybackProtocol PROTOCOL = PlaybackProtocol.HLS;
    private static final PlayerType PLAYER_TYPE = PlayerType.SKIPPY;
    private static final Long LATENCY = 1000L;
    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private static final String FORMAT = "opus";
    private static final int BITRATE = 128000;

    private TestEventBusV2 eventBus = new TestEventBusV2();

    private SkippyPerformanceReporter performanceReporter;

    @Before
    public void setUp() {
        performanceReporter = new SkippyPerformanceReporter(eventBus);
    }

    @Test
    public void reportsTimeToPlayEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_PLAY);

        performanceReporter.report(audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
    }

    @Test
    public void reportsTimeToSeekEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_SEEK);

        performanceReporter.report(audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
    }

    @Test
    public void reportsTimeToPlaylistEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_GET_PLAYLIST);

        performanceReporter.report(audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
    }

    @Test
    public void reportsCacheUsagePercentage() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.CACHE_USAGE_PERCENT);

        performanceReporter.report(audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_CACHE_USAGE_PERCENT);
    }

    @Test
    public void reportsTimeToLoadSkippyLibrary() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_LOAD_LIBRARY);

        performanceReporter.reportTimeToLoadLibrary(audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
    }

    @Test
    public void ignoresTimeToBufferEvents() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.TIME_TO_BUFFER);
        performanceReporter.report(audioPerformanceEvent, PlayerType.SKIPPY);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    @Test
    public void ignoresUninterruptedPlaytimeEvents() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType(PlaybackMetric.UNINTERRUPTED_PLAYTIME);
        performanceReporter.report(audioPerformanceEvent, PlayerType.SKIPPY);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    private AudioPerformanceEvent createAudioPerformanceEventWithType(PlaybackMetric metric) {
        return new AudioPerformanceEvent(metric, LATENCY, PROTOCOL.getValue(), CDN_HOST, FORMAT, BITRATE, null);
    }

    private void assertPerformanceEvent(PlaybackPerformanceEvent event, int metric) {
        assertThat(event.metric()).isEqualTo(metric);
        assertThat(event.metricValue()).isEqualTo(LATENCY);
        assertThat(event.playbackProtocol()).isEqualTo(PROTOCOL.getValue());
        assertThat(event.playerType()).isEqualTo(PLAYER_TYPE.getValue());
        assertThat(event.cdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.format()).isEqualTo(FORMAT);
        assertThat(event.bitrate()).isEqualTo(BITRATE);
    }
}