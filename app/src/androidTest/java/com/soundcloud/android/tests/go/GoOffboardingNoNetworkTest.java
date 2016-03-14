package com.soundcloud.android.tests.go;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.go.GoOffboardingScreen;
import com.soundcloud.android.tests.ActivityTest;

public class GoOffboardingNoNetworkTest extends ActivityTest<GoOffboardingActivity> {

    private GoOffboardingScreen screen;

    public GoOffboardingNoNetworkTest() {
        super(GoOffboardingActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        networkManagerClient.switchWifiOff();
        screen = new GoOffboardingScreen(solo);
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.forcePendingPlanDowngrade(getInstrumentation().getTargetContext());
    }

    @Override
    protected void logInHelper() {
        TestUser.htCreator.logIn(getInstrumentation().getTargetContext());
    }

    public void testCanRetryContinueOnNetworkErrors() throws Exception {
        screen.clickContinue();
        // continue button should turn into retry button
        assertThat(screen.retryButton().hasVisibility(), is(true));

        networkManagerClient.switchWifiOn();

        // retry button should turn back into continue button
        StreamScreen streamScreen = screen.clickRetry();
        assertTrue(streamScreen.isVisible());
    }

    public void testCanRetryResubscribeOnNetworkErrors() throws Exception {
        screen.clickResubscribe();
        // resubscribe button should turn into retry button
        assertThat(screen.retryButton().hasVisibility(), is(true));

        networkManagerClient.switchWifiOn();

        // retry button should turn back into resubscribe button
        StreamScreen streamScreen = screen.clickRetry();
        assertTrue(streamScreen.isVisible());
    }
}
