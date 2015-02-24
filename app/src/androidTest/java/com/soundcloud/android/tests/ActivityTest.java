package com.soundcloud.android.tests;

import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.LogCollector;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.helpers.networkmanager.NetworkManager;
import com.soundcloud.android.framework.observers.ToastObserver;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.MenuScreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Base class for activity tests. Sets up robotium (via {@link com.soundcloud.android.framework.Han} and handles
 * screenshots for test failures.
 */
public abstract class ActivityTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    protected String testCaseName;
    protected MenuScreen menuScreen;
    protected Waiter waiter;
    protected ToastObserver toastObserver;

    private Flag dependency;
    private boolean runBasedOnResource = true;

    protected Han solo;

    protected NetworkManager networkManager;

    public ActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Han(getInstrumentation());
        waiter = new Waiter(solo);
        observeToasts();
        networkManager = new NetworkManager(getInstrumentation().getContext());

        testCaseName = String.format("%s.%s", getClass().getName(), getName());
        LogCollector.startCollecting(testCaseName);
        Log.d("TESTSTART:", String.format("%s", testCaseName));

        menuScreen = new MenuScreen(solo);

        networkManager.bind();

        getActivity();

        super.setUp(); // do not move, this has to run after the above
    }

    @Override
    protected void tearDown() throws Exception {
        toastObserver.stopObserving();
        networkManager.switchWifiOn();
        networkManager.unbind();
        AccountAssistant.logOut(getInstrumentation());
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));

        if (solo != null) {
            solo.finishOpenedActivities();
        }
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
                Log.d(getClass().getSimpleName(), "killSelf:" + pi.processName + "," + pi.pid);
                android.os.Process.killProcess(pi.pid);
            }
        }
    }

    @Override
    protected void runTest() throws Throwable {
        if (shouldSkip()) {
            return;
        }
        try {
            super.runTest();
            LogCollector.markFileForDeletion();
        } catch (Throwable t) {
            solo.takeScreenshot(testCaseName);
            Log.w("Boom! Screenshot!", String.format("Captured screenshot for failed test: %s", testCaseName));
            throw t;
        }
    }

    protected void log(Object msg, Object... args) {
        Log.d(getClass().getSimpleName(), msg == null ? null : String.format(msg.toString(), args));
    }

    protected void setDependsOn(Flag dependency) {
        this.dependency = dependency;
    }

    protected void setRunBasedOnResource(int id) {
        runBasedOnResource = getInstrumentation().getContext().getResources().getBoolean(id);
    }

    protected boolean shouldSkip() {
        return !runBasedOnResource || (dependency != null && getFeatureFlags().isDisabled(dependency));
    }

    private FeatureFlags getFeatureFlags() {
        Resources res = getActivity().getResources();
        return new FeatureFlags(res);
    }

    public MenuScreen getMenuScreen() {
        return menuScreen;
    }

    public final void observeToasts() {
        toastObserver = new ToastObserver(solo);
        observeToastsHelper();
    }

    protected void observeToastsHelper() {
    }

    public Waiter getWaiter() {
        return waiter;
    }

    public Han getSolo() {
        return solo;
    }
}
