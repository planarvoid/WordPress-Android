package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Test;

public class PlayerFunctionsTest {

    final Urn trackUrn = Urn.forTrack(123L);

    @Test
    public void isNotVideoAdShouldReturnTrueForTrackStateTransitions() {
        final PlaybackStateTransition state = new PlaybackStateTransition(PlaybackState.BUFFERING,
                                                                          PlayStateReason.NONE,
                                                                          trackUrn, 0, 0);
        assertThat(PlayerFunctions.IS_NOT_VIDEO_AD.call(state)).isTrue();
    }

    @Test
    public void isNotVideoAdShouldReturnTrueForUrnNotSetStateTransitions() {
        final PlaybackStateTransition state = new PlaybackStateTransition(PlaybackState.BUFFERING,
                                                                          PlayStateReason.NONE,
                                                                          Urn.NOT_SET, 0, 0);
        assertThat(PlayerFunctions.IS_NOT_VIDEO_AD.call(state)).isTrue();
    }

    @Test
    public void isNotTrackShouldReturnFalseForVideoStateTransitions() {
        final PlaybackStateTransition state = new PlaybackStateTransition(
                PlaybackState.BUFFERING,
                PlayStateReason.NONE,
                Urn.forAd("dfp", "123"),
                0, 0,
                new TestDateProvider()
        );
        assertThat(PlayerFunctions.IS_NOT_VIDEO_AD.call(state)).isFalse();
    }
}
