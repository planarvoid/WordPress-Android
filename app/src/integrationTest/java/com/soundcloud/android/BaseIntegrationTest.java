package com.soundcloud.android;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static com.soundcloud.android.tests.NetworkMappings.addDefaultMapping;
import static org.junit.Assert.assertFalse;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.soundcloud.android.di.TestApiModule;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.junit.RepeatRule;
import com.soundcloud.android.mrlocallocal.MrLocalLocal;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.tests.NetworkMappings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class BaseIntegrationTest {

    @Rule public WireMockRule wireMockRule = new WireMockRule(config());
    @Rule public RepeatRule repeatRule = new RepeatRule();

    protected MrLocalLocal mrLocalLocal;

    private WireMockConfiguration config() {
        return options()
                .notifier(new ConsoleNotifier(false))
                .port(NetworkMappings.MOCK_API_PORT);
    }

    private final Flag[] requiredFlags;
    private final TestUser user;

    public BaseIntegrationTest(TestUser user, Flag... requiredFlags) {
        this.user = user;
        this.requiredFlags = requiredFlags;
    }

    @Before
    public void setUp() throws Exception {
        addDefaultMapping(getTargetContext(), wireMockRule, true);
        assertFeatureFlags();
        reset();
        mrLocalLocal = new MrLocalLocal(getInstrumentation().getContext(), wireMockRule, TestApiModule.EVENTS_URL);
        enableEventLoggerInstantFlush(getTargetContext());
    }

    private void reset() throws Exception {
        logout();
        SoundCloudApplication.getObjectGraph().experimentOperations().clear();
        login();
    }

    private void assertFeatureFlags() {
        FeatureFlagsHelper
                .create(getTargetContext())
                .assertEnabled(requiredFlags);
    }

    @After
    public void tearDown() throws Exception {
        logout();
    }

    private void logout() throws Exception {
        AccountAssistant.logOutWithAccountCleanup(getInstrumentation());
    }

    private void login() throws Exception {
        AccountAssistant.loginWith(getTargetContext(), user);
    }

    public void addMockedResponse(String url, String file) {
        final Resources resources = getInstrumentation().getContext().getResources();
        final String body = readBodyOfFile(resources, file);

        assertFalse(body.isEmpty());
        wireMockRule.addStubMapping(stubFor(any(urlPathEqualTo(url)).willReturn(aResponse().withBody(body))));
    }

    public void unrespondingNetwork() {
        wireMockRule.addStubMapping(stubFor(any(anyUrl()).willReturn(aResponse().withFixedDelay(2000000))));
    }

    public void noNetwork() {
        wireMockRule.resetToDefaultMappings();
        wireMockRule.stop();
    }

    private void enableEventLoggerInstantFlush(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                         .edit()
                         .putBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true)
                         .apply();
    }
}
