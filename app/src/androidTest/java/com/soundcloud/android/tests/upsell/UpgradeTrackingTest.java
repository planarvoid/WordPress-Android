package com.soundcloud.android.tests.upsell;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class UpgradeTrackingTest extends ActivityTest<MainActivity> {

    private static final String UPGRADE_TEST_SCENARIO = "specs/upgrade-tracking-test2.spec";

    public UpgradeTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableUpsell(getInstrumentation().getTargetContext());
    }

    @Test
    public void testUpgradePageEvents() throws Exception {
        MoreScreen moreScreen = mainNavHelper.goToMore();
        assertThat(moreScreen, is(visible()));

        waiter.waitTwoSeconds();

        mrLocalLocal.startEventTracking();

        UpgradeScreen upgradeScreen = moreScreen.clickSubscribe();
        assertThat(upgradeScreen, is(visible()));

        waiter.waitTwoSeconds();
        upgradeScreen.clickDefaultCheckout();

        mrLocalLocal.verify(UPGRADE_TEST_SCENARIO);
    }

}
