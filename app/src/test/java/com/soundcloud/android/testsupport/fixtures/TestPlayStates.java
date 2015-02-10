package com.soundcloud.android.testsupport.fixtures;

import static com.soundcloud.android.playback.service.Playa.*;

import com.soundcloud.android.model.Urn;

public class TestPlayStates {

    private static final Urn URN = Urn.forTrack(123L);

    public static StateTransition playing() {
        return new StateTransition(PlayaState.PLAYING, Reason.NONE, URN);
    }

    public static StateTransition playing(long position, long duration) {
        return new StateTransition(PlayaState.PLAYING, Reason.NONE, URN, position, duration);
    }

    public static StateTransition idle() {
        return new StateTransition(PlayaState.IDLE, Reason.NONE, URN);
    }

    public static StateTransition idleDefault() {
        return new StateTransition(PlayaState.IDLE, Reason.NONE, Urn.NOT_SET);
    }

    public static StateTransition complete() {
        return new StateTransition(PlayaState.IDLE, Reason.TRACK_COMPLETE, URN);
    }

    public static StateTransition buffering() {
        return new StateTransition(PlayaState.BUFFERING, Reason.NONE, URN);
    }

    public static StateTransition buffering(long position, long duration) {
        return new StateTransition(PlayaState.BUFFERING, Reason.NONE, URN, position, duration);
    }

    public static StateTransition error(Reason reason) {
        return new StateTransition(PlayaState.IDLE, reason, URN);
    }

}
