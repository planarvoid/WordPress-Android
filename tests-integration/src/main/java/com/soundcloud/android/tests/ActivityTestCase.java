package com.soundcloud.android.tests;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.screens.MenuScreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Base class for activity tests. Sets up robotium (via {@link Han} and handles
 * screenshots for test failures.
 */
public abstract class ActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    protected String testCaseName;
    protected ApplicationProperties applicationProperties;
    protected MenuScreen menuScreen;
    protected Waiter waiter;

    private Feature dependency;

    protected Han solo;

    public ActivityTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Han(getInstrumentation());
        waiter = new Waiter(solo);


        testCaseName = String.format("%s.%s", getClass().getName(), getName());
        LogCollector.startCollecting(testCaseName);
        Log.d("TESTSTART:", String.format("%s", testCaseName));

        getActivity();
        //TODO: Why? We cannot assume that menu is always visible on startup.
        menuScreen = new MenuScreen(solo);

        super.setUp(); // do not move, this has to run after the above


        getInstrumentation().getContext()
            .getSharedPreferences("showcase_internal", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("hasShot1", true)
            .commit();
        applicationProperties = new ApplicationProperties(getActivity().getResources());
    }

    @Override
    protected void tearDown() throws Exception {
        if (solo != null) {
            solo.finishOpenedActivities();
        }
        AccountAssistant.logOut(getInstrumentation());
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        solo = null;
        Log.d("TESTEND:", String.format("%s", testCaseName));
        LogCollector.stopCollecting();
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
        if (shouldSkip()) return;
        try {
            super.runTest();
            LogCollector.markFileForDeletion();
        }
        catch (Throwable t) {
            solo.takeScreenshot(testCaseName);
            Log.w("Boom! Screenshot!",String.format("Captured screenshot for failed test: %s", testCaseName));
            throw t;
        }
    }

    protected void log(Object msg, Object... args) {
        Log.d(getClass().getSimpleName(), msg == null ? null : String.format(msg.toString(), args));
    }

    protected void setDependsOn(Feature dependency) {
        this.dependency = dependency;
    }

    protected boolean shouldSkip() {
        return dependency != null && getFeatureFlags().isDisabled(dependency);
    }

    private FeatureFlags getFeatureFlags() {
        Resources res = getActivity().getResources();
        return new FeatureFlags(res);
    }

    public MenuScreen getMenuScreen() {
        return menuScreen;
    }

    public Waiter getWaiter() {
        return waiter;
    }

    public Han getSolo() {
        return solo;
    }
}
