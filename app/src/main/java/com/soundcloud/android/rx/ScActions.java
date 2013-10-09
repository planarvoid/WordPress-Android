package com.soundcloud.android.rx;

import rx.util.functions.Action1;

public class ScActions {

    public static final Action1 NO_OP = new Action1() {
        @Override
        public void call(Object o) {
        }
    };

}
