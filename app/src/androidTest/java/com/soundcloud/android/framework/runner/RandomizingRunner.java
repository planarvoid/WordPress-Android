package com.soundcloud.android.framework.runner;

import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;

public class RandomizingRunner extends InstrumentationTestRunner {
    protected AndroidTestRunner runner;

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        runner = super.getAndroidTestRunner();
        return runner;
    }
}
