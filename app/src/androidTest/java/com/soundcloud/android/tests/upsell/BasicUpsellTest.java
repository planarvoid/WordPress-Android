package com.soundcloud.android.tests.upsell;

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

    public void testNavDrawerUpsellImpressionAndClick() {
        UpgradeScreen upgradeScreen = menuScreen
                .open()
                .clickUpsell();

        assertThat(upgradeScreen.isVisible(), is(true));

        verifier.assertScenario(NAV_UPSELL_TEST_SCENARIO);
    }

    public void testSettingsUpsellImpressionAndClick() {
        HomeScreen homeScreen = new HomeScreen(solo);

        UpgradeScreen upgradeScreen = homeScreen.actionBar()
                .clickSettingsOverflowButton()
                .clickOfflineSettings()
                .clickSubscribe();

        assertThat(upgradeScreen.isVisible(), is(true));

        verifier.assertScenario(SETTINGS_UPSELL_TEST_SCENARIO);
    }

}
