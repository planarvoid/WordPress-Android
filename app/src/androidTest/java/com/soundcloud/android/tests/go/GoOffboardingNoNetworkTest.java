package com.soundcloud.android.tests.go;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE;
import static com.soundcloud.android.api.ApiEndpoints.CONFIGURATION;
import static com.soundcloud.android.framework.TestUser.htCreator;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.forcePendingPlanDowngrade;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.go.GoOffboardingScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class GoOffboardingNoNetworkTest extends ActivityTest<GoOffboardingActivity> {

    private GoOffboardingScreen screen;
    private StubMapping stubMapping;

    public GoOffboardingNoNetworkTest() {
        super(GoOffboardingActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        screen = new GoOffboardingScreen(solo);
    }

    @Override
    protected void addInitialStubMappings() {
        stubMapping = stubFor(get(urlPathMatching(CONFIGURATION.path()))
                                      .willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE)));
    }

    @Override
    protected void beforeActivityLaunched() {
        forcePendingPlanDowngrade(getInstrumentation().getTargetContext());
    }

    @Override
    protected TestUser getUserForLogin() {
        return htCreator;
    }

    @Test
    public void testCanRetryContinueOnNetworkErrors() throws Exception {
        screen.clickContinue();
        // continue button should turn into retry button
        assertThat(screen.retryButton().hasVisibility(), is(true));

        removeStub(stubMapping);

        // retry button should turn back into continue button
        StreamScreen streamScreen = screen.clickContinueRetry();
        assertTrue(streamScreen.isVisible());
    }

    @Test
    public void testCanRetryResubscribeOnNetworkErrors() throws Exception {
        screen.clickResubscribe();
        // resubscribe button should turn into retry button
        assertThat(screen.retryButton().hasVisibility(), is(true));

        removeStub(stubMapping);

        // retry button should turn back into resubscribe button
        final UpgradeScreen upgradeScreen = screen.clickResubscribeRetry();
        assertTrue(upgradeScreen.upgradeButton().hasVisibility());
    }
}
