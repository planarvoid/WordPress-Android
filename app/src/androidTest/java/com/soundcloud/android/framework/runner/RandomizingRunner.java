package com.soundcloud.android.framework.runner;

import com.android.test.runner.MultiDexTestRunner;
import com.soundcloud.android.tests.SoundCloudTestApplication;

import android.app.Application;
import android.content.Context;
import android.test.AndroidTestRunner;

public class RandomizingRunner extends MultiDexTestRunner {
    protected AndroidTestRunner runner;

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(cl, SoundCloudTestApplication.class.getName(), context);
    }

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
