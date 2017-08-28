package com.soundcloud.android.playback.flipper;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackItem;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PerformanceReporterTest {

    private static final PlaybackProtocol PROTOCOL = PlaybackProtocol.HLS;
    private static final PlayerType PLAYER_TYPE = PlayerType.FLIPPER;
    private static final Urn USER_URN = Urn.forUser(1234567L);
    private static final Long LATENCY = 1000L;
    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private static final String FORMAT = "opus";
    private static final int BITRATE = 128000;
    private static final ConnectionType CONNECTION_TYPE = ConnectionType.FOUR_G;

    @Mock private AccountOperations accountOperations;
    @Mock private ConnectionHelper connectionHelper;
    private TestEventBusV2 eventBus = new TestEventBusV2();

    private PerformanceReporter performanceReporter;

    @Before
    public void setUp() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(connectionHelper.getCurrentConnectionType()).thenReturn(CONNECTION_TYPE);

        performanceReporter = new PerformanceReporter(eventBus, accountOperations, connectionHelper);
    }

    @Test
    public void doesNotReportAdPerformance() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType("anyType");
        performanceReporter.report(TestPlaybackItem.audioAd(), audioPerformanceEvent, PlayerType.FLIPPER);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    @Test
    public void reportsTimeToPlayEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType("play");

        performanceReporter.report(TestPlaybackItem.audio(), audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
    }

    @Test
    public void reportsTimeToSeekEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType("seek");

        performanceReporter.report(TestPlaybackItem.audio(), audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
    }

    @Test
    public void reportsTimeToPlaylistEvent() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType("playlist");

        performanceReporter.report(TestPlaybackItem.audio(), audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
    }

    @Test
    public void reportsCacheUsagePercentage() {
        AudioPerformanceEvent audioPerformanceEvent = createAudioPerformanceEventWithType("cacheUsage");

        performanceReporter.report(TestPlaybackItem.audio(), audioPerformanceEvent, PLAYER_TYPE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertPerformanceEvent(event, PlaybackPerformanceEvent.METRIC_CACHE_USAGE_PERCENT);
    }

    private AudioPerformanceEvent createAudioPerformanceEventWithType(String type) {
        return new AudioPerformanceEvent(type, LATENCY, PROTOCOL.getValue(), CDN_HOST, FORMAT, BITRATE, null);
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
