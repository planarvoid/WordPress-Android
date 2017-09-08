package com.soundcloud.android.tests.go;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.forcePendingPlanUpgrade;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.matcher.view.IsVisible;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.go.GoOnboardingScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import org.hamcrest.Matcher;
import org.junit.Test;

public class GoOnboardingSubscribedUserTest extends ActivityTest<GoOnboardingActivity> {
    private GoOnboardingScreen screen;
    private StubMapping stubMapping;

    public GoOnboardingSubscribedUserTest() {
        super(GoOnboardingActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return offlineUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        screen = new GoOnboardingScreen(solo);
    }

    @Override
    protected void addInitialStubMappings() {
        stubMapping = stubFor(get(urlPathMatching("/configuration/android"))
                                      .willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE)));
    }

    @Override
    protected void beforeActivityLaunched() {
        forcePendingPlanUpgrade(getInstrumentation().getTargetContext());
    }

    @Test
    public void testGoStartLazyLoadingAndRetry() throws Exception {
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
        return visible();
    }

    private Matcher<ViewElement> viewVisible() {
        return IsVisible.visible();
    }

}
