package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayerFunctionsTest extends AndroidUnitTest {

    final Urn trackUrn = Urn.forTrack(123L);

    @Test
    public void isForTrackShouldReturnTrueForTrackStateTransitions() {
        final Player.StateTransition state = new Player.StateTransition(
                Player.PlayerState.BUFFERING,
                Player.Reason.NONE,
                trackUrn
        );
        assertThat(PlayerFunctions.IS_FOR_TRACK.call(state)).isTrue();
    }

    @Test
    public void isForTrackShouldReturnFalseForVideoStateTransitions() {
        final Player.StateTransition state = new Player.StateTransition(
                Player.PlayerState.BUFFERING,
                Player.Reason.NONE,
                "dfp-video-ad",
                0, 0,
                new TestDateProvider()
        );
        assertThat(PlayerFunctions.IS_FOR_TRACK.call(state)).isFalse();
    }
}