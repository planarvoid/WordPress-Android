package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;

public class BufferUnderrunListenerTest extends AndroidUnitTest {
    private final Urn track = Urn.forTrack(123L);
    private BufferUnderrunListener listener;
    private TestEventBus eventBus;
    @Mock private BufferUnderrunListener.Detector detector;
    @Mock private TestDateProvider dateProvider;
    @Mock private UninterruptedPlaytimeStorage uninterruptedPlaytimeStorage;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        listener = new BufferUnderrunListener(
                detector,
                eventBus,
                uninterruptedPlaytimeStorage,
                dateProvider);
    }

    @Test
    public void shouldNotSendUninterruptedPlaytimeEvent() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(), false);
        final List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();
    }

    @Test
    public void shouldSendUninterruptedPlaytimeEvent() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(100L), false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(1000L), true);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.getMetricValue()).isEqualTo(900L);
    }

    @Test
    public void shouldFilterBufferingEventOnSeekAndStart() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(100L), false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(1000L), false);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();
    }

    @Test
    public void shouldSaveUninterruptedPlaytimeOnIdle() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.IDLE, new Date(1000L), false);

        verify(uninterruptedPlaytimeStorage).setPlaytime(900L, PlayerType.SKIPPY);
    }

    @Test
    public void shouldSaveZeroedUninterruptedPlaytimeOnBufferUnderun() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(1000L), true);

        verify(uninterruptedPlaytimeStorage).setPlaytime(0L, PlayerType.SKIPPY);
    }

    @Test
    public void shouldIncrementOverExistingUninterruptedPlaytime() {
        when(uninterruptedPlaytimeStorage.getPlayTime(PlayerType.SKIPPY)).thenReturn(50L);

        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(1000L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(2000L), true);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.getMetricValue()).isEqualTo(1050L);
    }

    @Test
    public void shouldFilterPlayingAfterBufferUnderrun() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(1000L), true);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(1500), true);

        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);
    }

    @Test
    public void shouldFilterTimeCalculationOnPlayingAfterPlaying() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.PLAYING, new Date(1000L), false);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(PlayerType.SKIPPY, Player.PlayerState.BUFFERING, new Date(5000L), true);

        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.getMetricValue()).isEqualTo(4900L);
    }

    private void createAndProcessStateTransition(PlayerType player, Player.PlayerState newState, Date transitionTime, boolean isBufferUnderrun) {
        Player.StateTransition stateTransition = new Player.StateTransition(newState, Player.Reason.NONE, track);
        stateTransition.addExtraAttribute(Player.StateTransition.EXTRA_PLAYER_TYPE, player.getValue());
        when(detector.onStateTransitionEvent(stateTransition)).thenReturn(isBufferUnderrun);
        when(dateProvider.getCurrentDate()).thenReturn(transitionTime);
        listener.onPlaystateChanged(stateTransition, PlaybackProtocol.HLS, player, ConnectionType.THREE_G);
    }

}
