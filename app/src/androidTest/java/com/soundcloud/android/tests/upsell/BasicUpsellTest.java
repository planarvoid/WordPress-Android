package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.framework.matcher.view.IsVisible;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class BasicUpsellTest extends TrackingActivityTest<MainActivity> {

    private static final String SETTINGS_UPSELL_TEST_SCENARIO = "settings-upsell-tracking-test";

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

    public void testSettingsUpsellImpressionAndClick() {
        MoreScreen moreScreen = mainNavHelper.goToMore();
        startEventTracking();

        UpgradeScreen upgradeScreen = moreScreen.clickSubscribe();

        assertThat(upgradeScreen, is(visible()));
        assertThat(upgradeScreen.upgradeButton(), is(IsVisible.visible()));

        finishEventTracking(SETTINGS_UPSELL_TEST_SCENARIO);
    }

}
