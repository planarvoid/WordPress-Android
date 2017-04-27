package com.soundcloud.android.tests.go;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.go.GoOffboardingScreen;
import com.soundcloud.android.tests.ActivityTest;

public class GoOffboardingTrackingTest extends ActivityTest<GoOffboardingActivity> {

    private static final String TEST_SCENARIO = "specs/go-offboarding2.spec";
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

    public void ignore_testTrackResubscribeButtonClickAndImpression() throws Exception {
        final UpgradeScreen upgradeScreen = screen.clickResubscribe();
        assertThat(upgradeScreen, is(visible()));

        mrLocalLocal.verify(TEST_SCENARIO);
    }
}
