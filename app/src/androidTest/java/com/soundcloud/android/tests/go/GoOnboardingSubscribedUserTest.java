package com.soundcloud.android.tests.go;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.go.GoOnboardingScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import org.hamcrest.Matcher;

public class GoOnboardingSubscribedUserTest extends ActivityTest<GoOnboardingActivity> {
    private GoOnboardingScreen screen;
    private StubMapping stubMapping;

    public GoOnboardingSubscribedUserTest() {
        super(GoOnboardingActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        screen = new GoOnboardingScreen(solo);
    }

    @Override
    protected void addInitialStubMappings() {
        stubMapping = stubFor(get(urlPathMatching("/configuration/android"))
                                      .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.forcePendingPlanUpgrade(getInstrumentation().getTargetContext());
    }

    public void testGoStartLazyLoadingAndRetry() {
        CollectionScreen nextScreen;

        assertThat(screen.startButton(), is(viewVisible()));
        nextScreen = screen.clickStartButton();
        assertThat(nextScreen, is(not(screenVisible())));
        assertThat(screen.retryButton(), is(viewVisible()));

        removeStub(stubMapping);

        nextScreen = screen.clickRetryButton();
        assertThat(nextScreen, is(screenVisible()));
    }

    private Matcher<Screen> screenVisible() {
        return com.soundcloud.android.framework.matcher.screen.IsVisible.visible();
    }

    private Matcher<ViewElement> viewVisible() {
        return com.soundcloud.android.framework.matcher.view.IsVisible.visible();
    }

}
