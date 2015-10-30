package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.UpgradeScreen;

@EventTrackingTest
public class BasicUpsellTest extends TrackingActivityTest<MainActivity> {

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

    public void testSettingsUpsellImpressionAndClick() {
        startEventTracking();

        UpgradeScreen upgradeScreen = mainNavHelper.goToOfflineSettings()
                .clickSubscribe();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(SETTINGS_UPSELL_TEST_SCENARIO);
    }

}
