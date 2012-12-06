package com.soundcloud.android.tests;

import junit.framework.TestCase;

import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;

import java.util.Collections;
import java.util.List;

public class RandomizingRunner extends InstrumentationTestRunner {
    protected AndroidTestRunner runner;

    @Override
    public void onStart() {
        List<TestCase> testCases = runner.getTestCases();
        Collections.shuffle(testCases);

        super.onStart();
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        runner = super.getAndroidTestRunner();
        return runner;
    }
}
