package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Test;

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
        final PlayStateEvent event = PlayStateEvent.create(transition, 456L, true, "play-id");

        assertThat(event.getProgress()).isEqualTo(new PlaybackProgress(123, 456, dateProvider));
        assertThat(event.getTransition()).isEqualTo(transition);
        assertThat(event.isFirstPlay()).isEqualTo(true);
        assertThat(event.getPlayId()).isEqualTo("play-id");
    }
}
