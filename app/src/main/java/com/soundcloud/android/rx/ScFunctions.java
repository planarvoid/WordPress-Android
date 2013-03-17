package com.soundcloud.android.rx;

import rx.util.functions.Func0;

public class ScFunctions {

    public static AlwaysTrue alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    private enum AlwaysTrue implements Func0<Boolean> {
        INSTANCE;

        @Override
        public Boolean call() {
            return true;
        }
    }
}
