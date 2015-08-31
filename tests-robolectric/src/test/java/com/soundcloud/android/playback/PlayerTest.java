package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTest {

    @Test
    public void addsStateTransitionInsideIntent() {
        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.ERROR_FAILED, Urn.forTrack(1L));
        final Intent intent = new Intent();
        stateTransition.addToIntent(intent);
        expect(intent.getIntExtra(Player.PlayerState.PLAYER_STATE_EXTRA, -1)).toEqual(Player.PlayerState.BUFFERING.ordinal());
        expect(intent.getIntExtra(Player.Reason.PLAYER_REASON_EXTRA, -1)).toEqual(Player.Reason.ERROR_FAILED.ordinal());
    }

    @Test
    public void isForTrackIsFalseWithDifferentTrackUrn() throws Exception {
        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, Urn.forTrack(2L));
        expect(stateTransition.isForTrack(Urn.forTrack(1L))).toBeFalse();
    }

    @Test
    public void isForTrackIsTrueWithSameTrackUrn() throws Exception {
        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, Urn.forTrack(1L));
        expect(stateTransition.isForTrack(Urn.forTrack(1L))).toBeTrue();
    }
}
