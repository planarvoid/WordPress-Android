package com.soundcloud.android.tests.go;

import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.screens.go.GoOffboardingScreen;

public class GoOffboardingTrackingTest extends TrackingActivityTest<GoOffboardingActivity> {

    private static final String TEST_SCENARIO = "go-offboarding";
    private GoOffboardingScreen screen;

    public GoOffboardingTrackingTest() {
        super(GoOffboardingActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        screen = new GoOffboardingScreen(solo);
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.forcePendingPlanDowngrade(getInstrumentation().getTargetContext());
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.htCreator;
    }

    @Override
    protected void beforeLogIn() {
        super.beforeLogIn();
        startEventTracking();
    }

    public void testTrackResubscribeButtonClickAndImpression() throws Exception {
        screen.clickResubscribe();

        finishEventTracking(TEST_SCENARIO);
    }
}
