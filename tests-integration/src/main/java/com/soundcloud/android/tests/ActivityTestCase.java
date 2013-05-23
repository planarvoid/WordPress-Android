package com.soundcloud.android.tests;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.screens.MenuScreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * Base class for activity tests. Sets up robotium (via {@link Han} and handles
 * screenshots for test failures.
 */
public abstract class ActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    protected static final boolean EMULATOR = SoundCloudApplication.EMULATOR;
    protected MenuScreen menuScreen;

    protected Han solo;

    public ActivityTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Han(getInstrumentation(), getActivity());
        menuScreen = new MenuScreen(solo);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        if (solo != null) {
            solo.finishOpenedActivities();
        }
        super.tearDown();
        solo = null;
    }


    @SuppressWarnings("UnusedDeclaration")
    protected void killSelf() {
        ActivityManager activityManager = (ActivityManager)
                getInstrumentation().getTargetContext().getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo pi : activityManager.getRunningAppProcesses()) {
            if ("com.soundcloud.android".equals(pi.processName)) {
                Log.d(getClass().getSimpleName(), "killSelf:"+pi.processName+","+pi.pid);
                android.os.Process.killProcess(pi.pid);
            }
        }
    }

    protected void assertPackageNotInstalled(String pkg) {
        try {
            PackageInfo i = getInstrumentation().getTargetContext().getPackageManager().getPackageInfo(pkg, 0);
            fail("package "+i+ " should not be installed");
        } catch (PackageManager.NameNotFoundException e) {
            // good
        }
    }

    @Override
    protected void runTest() throws Throwable {
        try {
            super.runTest();
        } catch (Throwable e) {
            if (!(e instanceof OutOfMemoryError)) {
                solo.poseForScreenshot(getClass().getSimpleName()+"-"+getName());
            }
            throw e;
        }
    }

    protected void assertMatches(String pattern, String string) {
        assertTrue("String " + string + " doesn't match "+pattern , Pattern.compile(pattern).matcher(string).matches());
    }

    protected void log(Object msg, Object... args) {
        Log.d(getClass().getSimpleName(), msg == null ? null : String.format(msg.toString(), args));
    }
}
