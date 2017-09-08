package com.soundcloud.android.tests

import android.app.Activity
import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.util.Log
import android.widget.ProgressBar
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.soundcloud.android.BuildConfig
import com.soundcloud.android.di.TestApiModule
import com.soundcloud.android.framework.AccountAssistant
import com.soundcloud.android.framework.Han
import com.soundcloud.android.framework.TestUser
import com.soundcloud.android.framework.Waiter
import com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile
import com.soundcloud.android.framework.helpers.ConfigurationHelper
import com.soundcloud.android.framework.helpers.MainNavigationHelper
import com.soundcloud.android.framework.helpers.RunConditionsHelper
import com.soundcloud.android.framework.observers.ToastObserver
import com.soundcloud.android.framework.rules.LogHandlerRule
import com.soundcloud.android.framework.rules.RetryRule
import com.soundcloud.android.framework.rules.ScreenshotOnTestFailureRule
import com.soundcloud.android.framework.with.With
import com.soundcloud.android.mrlocallocal.MrLocalLocal
import com.soundcloud.android.properties.ExperimentsHelper
import com.soundcloud.android.properties.FeatureFlagsHelper
import com.soundcloud.android.settings.SettingKey
import com.soundcloud.android.utils.TestConnectionHelper
import com.soundcloud.android.utils.extensions.put
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Base class for activity tests.
 *
 * Sets up:
 * - Robotium (via [Han]) which is @deprecated
 * - Espresso (via [ActivityTestRule])
 * - WireMock for network stubbing concerns
 * - MrLocalLocal for verifying event tracking
 *
 * and handles screenshots for test failures.
 */
abstract class ActivityTest<T : Activity>
protected constructor(activityClass: Class<T>) {
    @JvmField @Rule val retryRule = RetryRule(0)
    @JvmField @Rule val activityTestRule: ActivityTestRule<T> = ActivityTestRule(activityClass, true, false)
    @JvmField @Rule val logHandlerRule = LogHandlerRule()
    @JvmField @Rule val screenshot = ScreenshotOnTestFailureRule()

    @JvmField protected val runConditions = RunConditionsHelper()
    protected val featureFlags: FeatureFlagsHelper
        get() = FeatureFlagsHelper.create(instrumentation.targetContext)
    protected val experiments: ExperimentsHelper
        get() = ExperimentsHelper.create(activityTestRule.activity.applicationContext)

    lateinit protected var toastObserver: ToastObserver
    lateinit var solo: Han
    lateinit protected var mainNavHelper: MainNavigationHelper
    lateinit var waiter: Waiter
    lateinit protected var connectionHelper: TestConnectionHelper
    lateinit private var wireMockServer: WireMockServer
    lateinit protected var mrLocalLocal: MrLocalLocal
    private var activityIntent: Intent? = null

    @Before
    @Throws(Exception::class)
    open fun setUp() {
        configureWiremock()

        addActivityMonitors(instrumentation)

        solo = Han(instrumentation).apply {
            registerBusyUiIndicator(With.classSimpleName(ProgressBar::class.java.simpleName))
            setup()
        }
        waiter = Waiter(solo)

        // Wait for SoundCloudApplication.onCreate() to run so that we can be sure it has been injected.
        // Otherwise there is a race where we might use it before injection, resulting in an NPE.
        val targetContext = instrumentation.targetContext
        val application = SoundCloudTestApplication.fromContext(targetContext)
        application.awaitOnCreate(10, TimeUnit.SECONDS)
        connectionHelper = application.getConnectionHelper().apply {
            isWifiConnected = true
            isNetworkConnected = true
        }

        AccountAssistant.logOutWithAccountCleanup(instrumentation)

        // the introductory overlay blocks interaction with the player, so disable it
        ConfigurationHelper.disableIntroductoryOverlays(targetContext)

        // Player pager nudge onboarding interferes with player tests, so disable it
        ConfigurationHelper.disablePagerOnboarding(targetContext)
        enableEventLoggerInstantFlush(targetContext)

        beforeLogIn()
        logIn()
        observeToasts()
        beforeActivityLaunched()

        // Applies run conditions which can lead to the test not being run
        runConditions.apply()
        activityTestRule.launchActivity(activityIntent)
        setDefaultLocale()

        mainNavHelper = MainNavigationHelper(solo)
    }

    protected fun setActivityIntent(intent: Intent) {
        this.activityIntent = intent
    }

    @After
    @Throws(Exception::class)
    open fun tearDown() {
        removeActivityMonitors(instrumentation)
        toastObserver.stopObserving()

        solo.finishOpenedActivities()

        AccountAssistant.logOutWithAccountCleanup(instrumentation)
        assertThat(AccountAssistant.getAccount(instrumentation.targetContext)).isNull()

        connectionHelper.isWifiConnected = true
        connectionHelper.isNetworkConnected = true

        stopWiremock()
    }

    private fun setDefaultLocale() {
        setLocale(Locale.US, activityTestRule.activity.resources)
    }

    private fun setLocale(locale: Locale, resources: Resources) {
        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun configureWiremock() {
        Log.d("WIREMOCK", "Initializing Wiremock")
        wireMockServer = NetworkMappings.create(instrumentation.targetContext, wiremockLoggingEnabled(), false)
        wireMockServer.start()

        addInitialStubMappings()
        mrLocalLocal = MrLocalLocal(instrumentation.context, wireMockServer, TestApiModule.EVENTS_URL)
    }

    protected fun addMockedResponse(url: String, file: String) {
        val resources = instrumentation.context.resources
        val body = readBodyOfFile(resources, file)

        Assert.assertFalse(body.isEmpty())
        wireMockServer.addStubMapping(stubFor(any(urlPathEqualTo(url)).willReturn(aResponse().withBody(body))))
    }

    protected fun addMockedResponse(url: String, statusCode: Int, file: String) {
        val resources = instrumentation.context.resources
        val body = readBodyOfFile(resources, file)

        Assert.assertFalse(body.isEmpty())
        wireMockServer.addStubMapping(stubFor(any(urlPathEqualTo(url)).willReturn(aResponse().withStatus(statusCode).withBody(body))))
    }

    protected fun addMockedStringResponse(url: String, statusCode: Int, response: String) {
        wireMockServer.addStubMapping(stubFor(any(urlPathEqualTo(url)).willReturn(aResponse().withStatus(statusCode).withBody(response))))
    }

    protected open fun wiremockLoggingEnabled() = false

    protected open fun addActivityMonitors(instrumentation: Instrumentation) {

    }

    /***
     * Add stubs for wiremock, e.g. :
     *
     * stubFor(get(urlPathEqualTo("/stream")).willReturn(aResponse().withStatus(500)));
     *
     * @see [http://wiremock.org/docs/stubbing/](http://wiremock.org/docs/stubbing/)
     */
    protected open fun addInitialStubMappings() {}

    protected open fun beforeActivityLaunched() {}

    protected fun beforeLogIn() {}

    protected open fun removeActivityMonitors(instrumentation: Instrumentation) {}

    protected fun stopWiremock() {
        wireMockServer.stop()
    }

    @Suppress("unused")
    protected fun killSelf() {
        val activityManager = instrumentation.targetContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (pi in activityManager.runningAppProcesses) {
            if (BuildConfig.APPLICATION_ID == pi.processName) {
                Log.d(javaClass.simpleName, "killSelf:" + pi.processName + "," + pi.pid)
                android.os.Process.killProcess(pi.pid)
            }
        }
    }

    protected fun log(msg: Any?, vararg args: Any) {
        Log.d(javaClass.simpleName, if (msg == null) null else String.format(msg.toString(), *args))
    }

    private fun logIn() {
        val testUser = getUserForLogin()
        if (testUser != null) {
            AccountAssistant.loginWith(instrumentation.targetContext, testUser)
        }
    }

    open protected fun getUserForLogin(): TestUser? = null

    private fun observeToasts() {
        toastObserver = ToastObserver(solo)
        observeToastsHelper()
    }

    private fun enableEventLoggerInstantFlush(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).put(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true)
    }

    protected open fun observeToastsHelper() {}

    protected fun resourceString(id: Int): String = activityTestRule.activity.getString(id)
    protected val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
}
