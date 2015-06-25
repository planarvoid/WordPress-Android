package com.soundcloud.android.framework.runner;

import com.android.test.runner.MultiDexTestRunner;

import android.test.AndroidTestRunner;

public class RandomizingRunner extends MultiDexTestRunner {
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
