package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.UninterruptedPlaytimeStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.utils.DateProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class BufferUnderrunListenerTest {
    private final Urn track = Urn.forTrack(123L);
    private BufferUnderrunListener listener;
    private TestEventBus eventBus;
    @Mock private BufferUnderrunListener.Detector detector;
    @Mock private DateProvider dateProvider;
    @Mock private UninterruptedPlaytimeStorage uninterruptedPlaytimeStorage;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        listener = new BufferUnderrunListener(detector,
                eventBus,
                uninterruptedPlaytimeStorage,
                dateProvider);
    }

    @Test
    public void shouldNotSendUninterruptedPlaytimeEvent() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(), false);
        final List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toBeEmpty();
    }

    @Test
    public void shouldSendUninterruptedPlaytimeEvent() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(100L), false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toBeEmpty();

        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(1000L), true);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toNumber(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        expect(event.getMetric()).toBe(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        expect(event.getMetricValue()).toEqual(900L);
    }

    @Test
    public void shouldFilterBufferingEventOnSeekAndStart() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(100L), false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toBeEmpty();

        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(1000L), false);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toBeEmpty();
    }

    @Test
    public void shouldSaveUninterruptedPlaytimeOnIdle() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.IDLE, new Date(1000L), false);

        verify(uninterruptedPlaytimeStorage).setPlaytime(900L, PlayerType.SKIPPY);
    }

    @Test
    public void shouldSaveZeroedUninterruptedPlaytimeOnBufferUnderun() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(1000L), true);

        verify(uninterruptedPlaytimeStorage).setPlaytime(0L, PlayerType.SKIPPY);
    }

    @Test
    public void shouldIncrementOverExistingUninterruptedPlaytime() {
        when(uninterruptedPlaytimeStorage.getPlayTime(PlayerType.SKIPPY)).thenReturn(50L);

        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(1000L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(2000L), true);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        expect(event.getMetric()).toBe(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        expect(event.getMetricValue()).toEqual(1050L);
    }

    @Test
    public void shouldFilterPlayingAfterBufferUnderrun() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(1000L), true);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toNumber(1);

        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(1500), true);

        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toNumber(1);
    }

    @Test
    public void shouldFilterTimeCalculationOnPlayingAfterPlaying() {
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.PLAYING, new Date(1000L), false);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toBeEmpty();

        createAndProcessStateTransition(PlayerType.SKIPPY, Playa.PlayaState.BUFFERING, new Date(5000L), true);

        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(playbackPerformanceEvents).toNumber(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        expect(event.getMetric()).toBe(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        expect(event.getMetricValue()).toEqual(4900L);
    }

    private void createAndProcessStateTransition(PlayerType player, Playa.PlayaState newState, Date transitionTime, boolean isBufferUnderrun) {
        Playa.StateTransition stateTransition = new Playa.StateTransition(newState, Playa.Reason.NONE, track);
        stateTransition.addExtraAttribute(Playa.StateTransition.EXTRA_PLAYER_TYPE, player.getValue());
        when(detector.onStateTransitionEvent(stateTransition)).thenReturn(isBufferUnderrun);
        when(dateProvider.getCurrentDate()).thenReturn(transitionTime);
        listener.onPlaystateChanged(stateTransition, PlaybackProtocol.HLS, player, ConnectionType.THREE_G);
    }

}