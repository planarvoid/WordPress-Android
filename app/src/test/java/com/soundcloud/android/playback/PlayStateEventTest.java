package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlayStateReason.ERROR_FAILED;
import static com.soundcloud.android.playback.PlayStateReason.PLAYBACK_COMPLETE;
import static com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions.buffering;
import static com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions.idle;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Test;

import android.support.annotation.NonNull;

public class PlayStateEventTest {

    private TestDateProvider dateProvider = new TestDateProvider();

    @Test
    public void returnsPlayStateWithOriginalDuration() {
        final PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE,
                                                                               TestPlayStates.URN, 123, 456, dateProvider);
        final PlayStateEvent event = PlayStateEvent.create(transition, 789L, true, "play-id");

        assertThat(event.getProgress()).isEqualTo(new PlaybackProgress(123, 456, dateProvider));
        assertThat(event.getTransition()).isEqualTo(transition);
        assertThat(event.isFirstPlay()).isEqualTo(true);
        assertThat(event.getPlayId()).isEqualTo("play-id");
    }

    @Test
    public void returnsPlayStateWithCorrectedDuration() {
        final PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE,
                                                                               TestPlayStates.URN, 123, 0, dateProvider);
        final PlayStateEvent event = getPlayStateEvent(transition);

        assertThat(event.getProgress()).isEqualTo(new PlaybackProgress(123, 456, dateProvider));
        assertThat(event.getTransition()).isEqualTo(transition);
        assertThat(event.isFirstPlay()).isEqualTo(true);
        assertThat(event.getPlayId()).isEqualTo("play-id");
    }

    @Test
    public void playSessionIsActiveWhileBuffering() {
        assertThat(getPlayStateEvent(buffering()).playSessionIsActive()).isTrue();
    }

    @Test
    public void playSessionIsActiveWhilePlaying() {
        assertThat(getPlayStateEvent(buffering()).playSessionIsActive()).isTrue();
    }

    @Test
    public void playSessionIsActiveTrackComplete() {
        assertThat(getPlayStateEvent(idle(PLAYBACK_COMPLETE)).playSessionIsActive()).isTrue();
    }

    @Test
    public void playSessionIsNotActiveAfterError() {
        assertThat(getPlayStateEvent(idle(ERROR_FAILED)).playSessionIsActive()).isFalse();
    }

    @Test
    public void playSessionIsNotActiveAfterPause() {
        assertThat(getPlayStateEvent(idle(ERROR_FAILED)).playSessionIsActive()).isFalse();
    }

    @Test
    public void playSessionIsNotActiveWhenPlayQueueComplete() {
        final PlayStateEvent playStateEvent = getPlayStateEvent(idle(PLAYBACK_COMPLETE));
        assertThat(PlayStateEvent.createPlayQueueCompleteEvent(playStateEvent).playSessionIsActive()).isFalse();
    }

    @NonNull
    private PlayStateEvent getPlayStateEvent(PlaybackStateTransition transition) {
        return PlayStateEvent.create(transition, 456L, true, "play-id");
    }
}
