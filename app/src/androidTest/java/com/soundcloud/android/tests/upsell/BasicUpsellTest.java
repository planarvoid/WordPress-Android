package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.matcher.view.IsVisible;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class BasicUpsellTest extends ActivityTest<MainActivity> {

    private static final String SETTINGS_UPSELL_TEST_SCENARIO = "specs/settings-upsell-tracking-test2.spec";

    public BasicUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    public void testSettingsUpsellImpressionAndClick() throws Exception {
        MoreScreen moreScreen = mainNavHelper.goToMore();
        waiter.waitTwoSeconds();
        mrLocalLocal.startEventTracking();

        UpgradeScreen upgradeScreen = moreScreen.clickSubscribe();

        assertThat(upgradeScreen, is(visible()));
        assertThat(upgradeScreen.upgradeButton(), is(IsVisible.visible()));

        mrLocalLocal.verify(SETTINGS_UPSELL_TEST_SCENARIO);
    }

}
