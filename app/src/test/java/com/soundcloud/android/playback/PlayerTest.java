package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.Intent;

public class PlayerTest extends AndroidUnitTest {

    @Test
    public void addsStateTransitionInsideIntent() {
        final Player.StateTransition stateTransition =
                new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.ERROR_FAILED, Urn.forTrack(1L));
        final Intent intent = new Intent();
        stateTransition.addToIntent(intent);

        assertThat(intent.getIntExtra(Player.PlayerState.PLAYER_STATE_EXTRA, -1))
                .isEqualTo(Player.PlayerState.BUFFERING.ordinal());
        assertThat(intent.getIntExtra(Player.Reason.PLAYER_REASON_EXTRA, -1))
                .isEqualTo(Player.Reason.ERROR_FAILED.ordinal());
    }

    @Test
    public void isForTrackIsFalseWithDifferentTrackUrn() {
        final Player.StateTransition stateTransition =
                new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, Urn.forTrack(2L));
        assertThat(stateTransition.isForUrn(Urn.forTrack(1L))).isFalse();
    }

    @Test
    public void isForTrackIsTrueWithSameTrackUrn() {
        final Player.StateTransition stateTransition =
                new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, Urn.forTrack(1L));
        assertThat(stateTransition.isForUrn(Urn.forTrack(1L))).isTrue();
    }
}
