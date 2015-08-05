package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class BasicUpsellTest extends TrackingActivityTest<MainActivity> {

    private static final String NAV_UPSELL_TEST_SCENARIO = "nav-upsell-tracking-test";
    private static final String SETTINGS_UPSELL_TEST_SCENARIO = "settings-upsell-tracking-test";

    public BasicUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.OFFLINE_SYNC);
        super.setUp();
    }

    public void ignore_testNavDrawerUpsellImpressionAndClick() {
        startEventTracking();

        UpgradeScreen upgradeScreen = menuScreen
                .open()
                .clickUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(NAV_UPSELL_TEST_SCENARIO);
    }

    public void ignore_testSettingsUpsellImpressionAndClick() {
        startEventTracking();

        HomeScreen homeScreen = new HomeScreen(solo);

        UpgradeScreen upgradeScreen = homeScreen.actionBar()
                .clickSettingsOverflowButton()
                .clickOfflineSettings()
                .clickSubscribe();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(SETTINGS_UPSELL_TEST_SCENARIO);
    }

}
