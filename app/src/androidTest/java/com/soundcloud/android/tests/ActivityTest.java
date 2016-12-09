package com.soundcloud.android.tests;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.LogCollector;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.helpers.MainNavigationHelper;
import com.soundcloud.android.framework.observers.ToastObserver;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.androidnetworkmanagerclient.NetworkManagerClient;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ProgressBar;

/**
 * Base class for activity tests. Sets up robotium (via {@link com.soundcloud.android.framework.Han} and handles
 * screenshots for test failures.
 */
public abstract class ActivityTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    private String testCaseName;
    private boolean runBasedOnTestResource = true;
    private Flag[] requiredEnabledFeatures;
    private Flag[] requiredDisabledFeatures;

    protected Han solo;
    protected MainNavigationHelper mainNavHelper;
    protected Waiter waiter;
    protected ToastObserver toastObserver;
    protected NetworkManagerClient networkManagerClient;

    public ActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Han(getInstrumentation());
        solo.registerBusyUiIndicator(With.classSimpleName(ProgressBar.class.getSimpleName()));
        solo.setup();
        waiter = new Waiter(solo);

        AccountAssistant.logOut(getInstrumentation());
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.APPBOY);
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));

        networkManagerClient = new NetworkManagerClient(getInstrumentation().getContext());

        testCaseName = String.format("%s.%s", getClass().getName(), getName());
        LogCollector.startCollecting(getInstrumentation().getTargetContext(), testCaseName);
        Log.d("TESTSTART:", String.format("%s", testCaseName));

        networkManagerClient.bind();
        networkManagerClient.switchWifiOn();

        beforeLogIn();
        logIn();
        observeToasts();
        beforeStartActivity();
        getActivity();


        mainNavHelper = new MainNavigationHelper(solo);

        super.setUp(); // do not move, this has to run after the above
    }

    protected void beforeStartActivity() {
    }

    protected void beforeLogIn() {
    }

    @Override
    protected void tearDown() throws Exception {
        toastObserver.stopObserving();

        AccountAssistant.logOutWithAccountCleanup(getInstrumentation());
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));

        if (solo != null) {
            solo.finishOpenedActivities();
        }
        networkManagerClient.switchWifiOn();
        networkManagerClient.unbind();
        solo = null;
        Log.d("TESTEND:", String.format("%s", testCaseName));
        LogCollector.stopCollecting();
    }


    @SuppressWarnings("UnusedDeclaration")
    protected void killSelf() {
        ActivityManager activityManager = (ActivityManager)
                getInstrumentation().getTargetContext().getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo pi : activityManager.getRunningAppProcesses()) {
            if (BuildConfig.APPLICATION_ID.equals(pi.processName)) {
                Log.d(getClass().getSimpleName(), "killSelf:" + pi.processName + "," + pi.pid);
                android.os.Process.killProcess(pi.pid);
            }
        }
    }

    @Override
    protected void runTest() throws Throwable {
        if (shouldRunTest()) {
            try {
                super.runTest();
                LogCollector.markFileForDeletion();
            } catch (Throwable t) {
                solo.takeScreenshot(testCaseName);
                Log.w("Boom! Screenshot!", String.format("Captured screenshot for failed test: %s", testCaseName));
                throw t;
            }
        }
    }

    protected void log(Object msg, Object... args) {
        Log.d(getClass().getSimpleName(), msg == null ? null : String.format(msg.toString(), args));
    }

    protected void setRequiredEnabledFeatures(Flag... requiredEnabledFeatures) {
        this.requiredEnabledFeatures = requiredEnabledFeatures;
    }

    protected void setRequiredDisabledFeatures(Flag... requiredDisabledFeatures) {
        this.requiredDisabledFeatures = requiredDisabledFeatures;
    }

    protected void setRunBasedOnTestResource(boolean runBasedOnTestResource) {
        this.runBasedOnTestResource = runBasedOnTestResource;
    }

    private boolean shouldRunTest() {
        return runBasedOnTestResource && requiredEnabledFeaturesAreEnabled() && requiredDisabledFeaturesAreDisabled();
    }

    private boolean requiredEnabledFeaturesAreEnabled() {
        if (requiredEnabledFeatures != null) {
            for (Flag dependency : requiredEnabledFeatures) {
                if (getFeatureFlags().isDisabled(dependency)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean requiredDisabledFeaturesAreDisabled() {
        if (requiredDisabledFeatures != null) {
            for (Flag disabledDependency : requiredDisabledFeatures) {
                if (getFeatureFlags().isEnabled(disabledDependency)) {
                    return false;
                }
            }
        }
        return true;
    }

    private FeatureFlagsHelper getFeatureFlags() {
        return FeatureFlagsHelper.create(getActivity().getApplicationContext());
    }

    protected final void logIn() {

        logInHelper();
    }

    protected void logInHelper() {
    }

    private void observeToasts() {
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

    protected String ressourceString(int id) {
        return getActivity().getString(id);
    }
}
