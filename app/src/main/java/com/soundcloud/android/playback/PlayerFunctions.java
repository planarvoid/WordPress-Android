package com.soundcloud.android.playback;

import rx.functions.Func1;

public class PlayerFunctions {

    private PlayerFunctions() {}

    public static final Func1<PlaybackStateTransition, Boolean> IS_NOT_VIDEO_AD = new Func1<PlaybackStateTransition, Boolean>() {
        @Override
        public Boolean call(PlaybackStateTransition currentState) {
            return !currentState.getUrn().isAd();
        }
    };
}
