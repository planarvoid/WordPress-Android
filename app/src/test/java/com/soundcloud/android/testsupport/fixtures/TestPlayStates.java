package com.soundcloud.android.testsupport.fixtures;


import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player.PlayerState;
import com.soundcloud.android.playback.Player.Reason;
import com.soundcloud.android.playback.Player.StateTransition;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.TestDateProvider;

public class TestPlayStates {

    private static final Urn URN = Urn.forTrack(123L);

    public static StateTransition playing() {
        return new StateTransition(PlayerState.PLAYING, Reason.NONE, URN);
    }

    public static StateTransition playing(long position, long duration) {
        return new StateTransition(PlayerState.PLAYING, Reason.NONE, URN, position, duration);
    }

    public static StateTransition playing(long position, long duration, CurrentDateProvider dateProvider) {
        return new StateTransition(PlayerState.PLAYING, Reason.NONE, URN, position, duration, dateProvider);
    }

    public static StateTransition idle() {
        return new StateTransition(PlayerState.IDLE, Reason.NONE, URN);
    }

    public static StateTransition idleDefault() {
        return new StateTransition(PlayerState.IDLE, Reason.NONE, Urn.NOT_SET);
    }

    public static StateTransition complete() {
        return new StateTransition(PlayerState.IDLE, Reason.TRACK_COMPLETE, URN);
    }

    public static StateTransition buffering() {
        return new StateTransition(PlayerState.BUFFERING, Reason.NONE, URN);
    }

    public static StateTransition buffering(TestDateProvider dateProvider) {
        return new StateTransition(PlayerState.BUFFERING, Reason.NONE, URN, 0, 0, dateProvider);
    }

    public static StateTransition buffering(long position, long duration) {
        return new StateTransition(PlayerState.BUFFERING, Reason.NONE, URN, position, duration);
    }

    public static StateTransition error(Reason reason) {
        return new StateTransition(PlayerState.IDLE, reason, URN);
    }
}
