package com.soundcloud.android.testsupport;

import java.util.concurrent.Callable;

public class TestSyncer implements Callable<Boolean> {

    private final boolean hasChanged = true;
    private boolean hasRun = false;

    @Override
    public Boolean call() throws Exception {
        hasRun = true;
        return hasChanged;
    }

    public boolean hasRun() {
        return hasRun;
    }
}
