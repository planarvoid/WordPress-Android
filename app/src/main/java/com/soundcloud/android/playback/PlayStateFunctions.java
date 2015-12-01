package com.soundcloud.android.playback;

import rx.functions.Func1;

public class PlayStateFunctions {

    public static final Func1<Player.StateTransition, Boolean> IS_NOT_DEFAULT_STATE = new Func1<Player.StateTransition, Boolean>() {
        @Override
        public Boolean call(Player.StateTransition stateTransition) {
            return !Player.StateTransition.DEFAULT.equals(stateTransition);
        }
    };

}
