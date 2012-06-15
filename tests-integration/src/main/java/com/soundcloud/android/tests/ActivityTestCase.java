package com.soundcloud.android.tests;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Base class for activity tests. Sets up robotium (via {@link Han} and handles
 * screenshots for test failures.
 */
public abstract class ActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    protected Han solo;

    public ActivityTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Han(getInstrumentation(), getActivity());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        if (solo != null) {
            solo.finishOpenedActivities();
        }
        super.tearDown();
    }

    @Override
    protected void runTest() throws Throwable {
        try {
            super.runTest();
        } catch (Error e) {
            solo.takeScreenshot();
            throw e;
        }
    }
}
