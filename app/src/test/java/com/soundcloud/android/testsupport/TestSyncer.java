package com.soundcloud.android.testsupport;

import java.util.concurrent.Callable;

public class TestSyncer implements Callable<Boolean> {

    private final boolean hasChanged = true;

    @Override
    public Boolean call() throws Exception {
        return hasChanged;
    }
}
