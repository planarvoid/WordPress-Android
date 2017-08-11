package com.soundcloud.android.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.di.TestApiModule;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.LogCollector;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.MainNavigationHelper;
import com.soundcloud.android.framework.observers.ToastObserver;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.mrlocallocal.MrLocalLocal;
import com.soundcloud.android.properties.ExperimentsHelper;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.utils.TestConnectionHelper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
    protected TestConnectionHelper connectionHelper;
    private WireMockServer wireMockServer;
    protected MrLocalLocal mrLocalLocal;

    public ActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        configureWiremock();

        addActivityMonitors(getInstrumentation());

        solo = new Han(getInstrumentation());
        solo.registerBusyUiIndicator(With.classSimpleName(ProgressBar.class.getSimpleName()));
        solo.setup();
        waiter = new Waiter(solo);

        // Wait for SoundCloudApplication.onCreate() to run so that we can be sure it has been injected.
        // Otherwise there is a race where we might use it before injection, resulting in an NPE.
        Context targetContext = getInstrumentation().getTargetContext();
        final SoundCloudTestApplication application = SoundCloudTestApplication.fromContext(targetContext);
        application.awaitOnCreate(10, TimeUnit.SECONDS);
        connectionHelper = application.getConnectionHelper();
        connectionHelper.setWifiConnected(true);
        connectionHelper.setNetworkConnected(true);

        AccountAssistant.logOutWithAccountCleanup(getInstrumentation());

        testCaseName = String.format("%s.%s", getClass().getName(), getName());
        LogCollector.startCollecting(targetContext, testCaseName);
        Log.d("TESTSTART:", String.format("%s", testCaseName));

        // the introductory overlay blocks interaction with the player, so disable it
        ConfigurationHelper.disableIntroductoryOverlays(targetContext);

        // Player pager nudge onboarding interferes with player tests, so disable it
        ConfigurationHelper.disablePagerOnboarding(targetContext);
        enableEventLoggerInstantFlush(targetContext);

        beforeLogIn();
        logIn();
        observeToasts();
        beforeStartActivity();
        getActivity();
        setDefaultLocale();

        mainNavHelper = new MainNavigationHelper(solo);

        super.setUp(); // do not move, this has to run after the above
    }

    private void setDefaultLocale() {
        setLocale(Locale.US, getActivity().getResources());
    }

    private void setLocale(Locale locale, Resources resources) {
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    private void configureWiremock() {
        Log.d("WIREMOCK", "Initializing Wiremock");
        wireMockServer = NetworkMappings.create(getInstrumentation().getTargetContext(), wiremockLoggingEnabled(), false);
        wireMockServer.start();

        addInitialStubMappings();
        mrLocalLocal = new MrLocalLocal(getInstrumentation().getContext(), wireMockServer, TestApiModule.EVENTS_URL);
    }

    protected void addMockedResponse(String url, String file) {
        final Resources resources = getInstrumentation().getContext().getResources();
        final String body = readBodyOfFile(resources, file);

        assertFalse(body.isEmpty());
        wireMockServer.addStubMapping(stubFor(any(urlPathEqualTo(url)).willReturn(aResponse().withBody(body))));
    }

    protected void addMockedResponse(String url, int statusCode, String file) {
        final Resources resources = getInstrumentation().getContext().getResources();
        final String body = readBodyOfFile(resources, file);

        assertFalse(body.isEmpty());
        wireMockServer.addStubMapping(stubFor(any(urlPathEqualTo(url)).willReturn(aResponse().withStatus(statusCode).withBody(body))));
    }

    protected boolean wiremockLoggingEnabled() {
        return false;
    }

    protected void addActivityMonitors(Instrumentation instrumentation) {

    }

    /***
     * Add stubs for wiremock, e.g. :
     *
     * stubFor(get(urlPathEqualTo("/stream")).willReturn(aResponse().withStatus(500)));
     *
     * @see <a href="http://wiremock.org/docs/stubbing/">http://wiremock.org/docs/stubbing/</a>
     */
    protected void addInitialStubMappings() {
    }

    protected void beforeStartActivity() {
    }

    protected void beforeLogIn() {
    }

    @Override
    protected void tearDown() throws Exception {
        removeActivityMonitors(getInstrumentation());
        toastObserver.stopObserving();

        if (solo != null) {
            solo.finishOpenedActivities();
        }

        AccountAssistant.logOutWithAccountCleanup(getInstrumentation());
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));

        connectionHelper.setWifiConnected(true);
        connectionHelper.setNetworkConnected(true);

        stopWiremock();
        solo = null;
        Log.d("TESTEND:", String.format("%s", testCaseName));
        LogCollector.stopCollecting();
    }

    protected void removeActivityMonitors(Instrumentation instrumentation) {

    }

    protected void stopWiremock() {
        wireMockServer.stop();
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
        return requiredDisabledFeatures == null || getFeatureFlags().isLocallyEnabled(requiredDisabledFeatures);
    }

    private boolean requiredDisabledFeaturesAreDisabled() {
        return requiredDisabledFeatures == null || getFeatureFlags().isLocallyDisabled(requiredDisabledFeatures);
    }

    protected FeatureFlagsHelper getFeatureFlags() {
        return FeatureFlagsHelper.create(getInstrumentation().getTargetContext());
    }

    protected ExperimentsHelper getExperiments() {
        return ExperimentsHelper.create(getActivity().getApplicationContext());
    }

    private void logIn() {
        TestUser testUser = getUserForLogin();
        if (testUser != null) {
            AccountAssistant.loginWith(getInstrumentation().getTargetContext(), testUser);
        }
    }

    protected TestUser getUserForLogin() {
        return null;
    }

    private void observeToasts() {
        toastObserver = new ToastObserver(solo);
        observeToastsHelper();
    }

    private void enableEventLoggerInstantFlush(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                         .edit()
                         .putBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true)
                         .apply();
    }

    protected void observeToastsHelper() {
    }

    public Waiter getWaiter() {
        return waiter;
    }

    public Han getSolo() {
        return solo;
    }

    protected String resourceString(int id) {
        return getActivity().getString(id);
    }
}
