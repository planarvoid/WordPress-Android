package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayerFunctionsTest extends AndroidUnitTest {

    final Urn trackUrn = Urn.forTrack(123L);

    @Test
    public void isNotVideoAdShouldReturnTrueForTrackStateTransitions() {
        final Player.StateTransition state = new Player.StateTransition(
                Player.PlayerState.BUFFERING,
                Player.Reason.NONE,
                trackUrn
        );
        assertThat(PlayerFunctions.IS_NOT_VIDEO_AD.call(state)).isTrue();
    }

    @Test
    public void isNotVideoAdShouldReturnTrueForUrnNotSetStateTransitions() {
        final Player.StateTransition state = new Player.StateTransition(
                Player.PlayerState.BUFFERING,
                Player.Reason.NONE,
                Urn.NOT_SET
        );
        assertThat(PlayerFunctions.IS_NOT_VIDEO_AD.call(state)).isTrue();
    }

    @Test
    public void isNotTrackShouldReturnFalseForVideoStateTransitions() {
        final Player.StateTransition state = new Player.StateTransition(
                Player.PlayerState.BUFFERING,
                Player.Reason.NONE,
                Urn.forAd("dfp", "123"),
                0, 0,
                new TestDateProvider()
        );
        assertThat(PlayerFunctions.IS_NOT_VIDEO_AD.call(state)).isFalse();
    }
}