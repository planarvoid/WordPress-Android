package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PlayaTest {

    @Test(expected = IllegalStateException.class)
    public void throwsReasonNoFoundInIntentException() throws Exception {
        Playa.StateTransition.fromIntent(new Intent());
    }

    @Test
    public void addsStateTransitionInsideIntent() {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.ERROR_FAILED);
        final Intent intent = new Intent();
        stateTransition.addToIntent(intent);
        expect(intent.getIntExtra(Playa.PlayaState.PLAYER_STATE_EXTRA, -1)).toEqual(Playa.PlayaState.BUFFERING.ordinal());
        expect(intent.getIntExtra(Playa.Reason.PLAYER_REASON_EXTRA, -1)).toEqual(Playa.Reason.ERROR_FAILED.ordinal());
    }

    @Test
    public void extractsStateTransitionFromIntent() {
        Intent intent = new Intent();
        intent.putExtra(Playa.PlayaState.PLAYER_STATE_EXTRA, Playa.PlayaState.BUFFERING.ordinal());
        intent.putExtra(Playa.Reason.PLAYER_REASON_EXTRA, Playa.Reason.ERROR_FAILED.ordinal());

        final Playa.StateTransition actual = Playa.StateTransition.fromIntent(intent);
        expect(actual.getNewState()).toEqual(Playa.PlayaState.BUFFERING);
        expect(actual.getReason()).toEqual(Playa.Reason.ERROR_FAILED);
    }
}
