package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PlayaTest {

    @Test
    public void addsStateTransitionInsideIntent() {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.ERROR_FAILED, Urn.forTrack(1L));
        final Intent intent = new Intent();
        stateTransition.addToIntent(intent);
        expect(intent.getIntExtra(Playa.PlayaState.PLAYER_STATE_EXTRA, -1)).toEqual(Playa.PlayaState.BUFFERING.ordinal());
        expect(intent.getIntExtra(Playa.Reason.PLAYER_REASON_EXTRA, -1)).toEqual(Playa.Reason.ERROR_FAILED.ordinal());
    }

    @Test
    public void isForTrackIsFalseWithDifferentTrackUrn() throws Exception {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, Urn.forTrack(2L));
        expect(stateTransition.isForTrack(Urn.forTrack(1L))).toBeFalse();
    }

    @Test
    public void isForTrackIsTrueWithSameTrackUrn() throws Exception {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, Urn.forTrack(1L));
        expect(stateTransition.isForTrack(Urn.forTrack(1L))).toBeTrue();
    }
}
