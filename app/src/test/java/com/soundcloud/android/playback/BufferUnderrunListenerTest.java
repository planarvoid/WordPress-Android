package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackItem;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class BufferUnderrunListenerTest {
    private Urn track;
    private BufferUnderrunListener listener;
    private TestEventBus eventBus;
    @Mock private BufferUnderrunListener.Detector detector;
    @Mock private TestDateProvider dateProvider;
    @Mock private UninterruptedPlaytimeStorage uninterruptedPlaytimeStorage;

    @Before
    public void setUp() throws Exception {
        track = Urn.forTrack(123L);
        eventBus = new TestEventBus();
        listener = new BufferUnderrunListener(
                detector,
                eventBus,
                uninterruptedPlaytimeStorage,
                dateProvider);
    }

    @Test
    public void shouldNotSendUninterruptedPlaytimeEvent() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(), false);
        final List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();
    }

    @Test
    public void shouldSendUninterruptedPlaytimeEvent() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(100L), false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(1000L), true);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(900L);
    }

    @Test
    public void shouldSendUninterruptedPlaytimeEventForAdWithFormatAndBitrate() {
        PlaybackItem playbackItem = VideoAdPlaybackItem.create(AdFixtures.getVideoAd(track), 0L);
        createAndProcessStateTransition(playbackItem,
                                        PlayerType.MediaPlayer.INSTANCE,
                                        PlaybackState.PLAYING,
                                        "video/mp4",
                                        1001000,
                                        new Date(100L),
                                        false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(playbackItem,
                                        PlayerType.MediaPlayer.INSTANCE,
                                        PlaybackState.BUFFERING,
                                        "video/mp4",
                                        1001000,
                                        new Date(1000L),
                                        true);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(900L);
        assertThat(event.bitrate()).isEqualTo(1001000);
        assertThat(event.format()).isEqualTo(PlaybackConstants.MIME_TYPE_MP4);
    }

    @Test
    public void shouldFilterBufferingEventOnSeekAndStart() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(100L), false);
        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(1000L), false);
        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();
    }

    @Test
    public void shouldSaveUninterruptedPlaytimeOnIdle() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.IDLE, new Date(1000L), false);

        verify(uninterruptedPlaytimeStorage).setPlaytime(900L, PlayerType.Skippy.INSTANCE);
    }

    @Test
    public void shouldSaveZeroedUninterruptedPlaytimeOnBufferUnderun() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(1000L), true);

        verify(uninterruptedPlaytimeStorage).setPlaytime(0L, PlayerType.Skippy.INSTANCE);
    }

    @Test
    public void shouldIncrementOverExistingUninterruptedPlaytime() {
        when(uninterruptedPlaytimeStorage.getPlayTime(PlayerType.Skippy.INSTANCE)).thenReturn(50L);

        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(1000L), false);
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(2000L), true);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(1050L);
    }

    @Test
    public void shouldFilterPlayingAfterBufferUnderrun() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(1000L), true);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(1500), true);

        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);
    }

    @Test
    public void shouldFilterTimeCalculationOnPlayingAfterPlaying() {
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(100L), false);
        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.PLAYING, new Date(1000L), false);

        List<PlaybackPerformanceEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).isEmpty();

        createAndProcessStateTransition(PlayerType.Skippy.INSTANCE, PlaybackState.BUFFERING, new Date(5000L), true);

        playbackPerformanceEvents = eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(playbackPerformanceEvents).hasSize(1);

        PlaybackPerformanceEvent event = playbackPerformanceEvents.get(0);
        assertThat(event.metric()).isEqualTo(PlaybackPerformanceEvent.METRIC_UNINTERRUPTED_PLAYTIME_MS);
        assertThat(event.metricValue()).isEqualTo(4900L);
    }

    private void createAndProcessStateTransition(PlayerType player,
                                                 PlaybackState newState,
                                                 Date transitionTime,
                                                 boolean isBufferUnderrun) {
        createAndProcessStateTransition(track,
                                        player,
                                        newState,
                                        PlaybackConstants.MediaType.UNKNOWN,
                                        0,
                                        transitionTime,
                                        isBufferUnderrun);
    }

    private void createAndProcessStateTransition(Urn itemUrn,
                                                 PlayerType player,
                                                 PlaybackState newState,
                                                 String format,
                                                 int bitrate,
                                                 Date transitionTime,
                                                 boolean isBufferUnderrun) {
        createAndProcessStateTransition(TestPlaybackItem.audio(itemUrn), player, newState, format, bitrate, transitionTime, isBufferUnderrun);
    }

    private void createAndProcessStateTransition(PlaybackItem playbackItem,
                                                 PlayerType player,
                                                 PlaybackState newState,
                                                 String format,
                                                 int bitrate,
                                                 Date transitionTime,
                                                 boolean isBufferUnderrun) {
        final PlaybackStateTransition stateTransition = new PlaybackStateTransition(newState,
                                                                                    PlayStateReason.NONE,
                                                                                    playbackItem.getUrn(),
                                                                                    0,
                                                                                    0,
                                                                                    format,
                                                                                    bitrate,
                                                                                    dateProvider);
        stateTransition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, player.getValue());
        when(detector.onStateTransitionEvent(stateTransition)).thenReturn(isBufferUnderrun);
        when(dateProvider.getCurrentDate()).thenReturn(transitionTime);
        listener.onPlaystateChanged(stateTransition, PlaybackProtocol.HLS, player);
    }

}
