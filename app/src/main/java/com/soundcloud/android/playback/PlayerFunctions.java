package com.soundcloud.android.playback;

import rx.functions.Func1;

public class PlayerFunctions {

    private PlayerFunctions() {}

    public static final Func1<Player.StateTransition, Boolean> IS_NOT_VIDEO_AD = new Func1<Player.StateTransition, Boolean>() {
        @Override
        public Boolean call(Player.StateTransition currentState) {
            return !currentState.getUrn().isAd();
        }
    };
}
