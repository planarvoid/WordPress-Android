package com.soundcloud.android.tests.go;

import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.screens.go.GoOffboardingScreen;

@EventTrackingTest
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
    protected void logInHelper() {
        TestUser.freeNonMonetizedUser.logIn(getInstrumentation().getTargetContext());
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
