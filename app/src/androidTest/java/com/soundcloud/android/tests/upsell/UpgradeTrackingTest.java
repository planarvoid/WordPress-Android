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
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.ToolBarElement;

@EventTrackingTest
public class UpgradeTrackingTest extends TrackingActivityTest<MainActivity> {

    private static final String UPGRADE_TEST_SCENARIO = "upgrade-tracking-test";

    public UpgradeTrackingTest() {
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

    // Ignored until we fix: https://github.com/soundcloud/SoundCloud-Android/issues/3202
    public void ignore_testUpgradePageEvents() {
        ToolBarElement toolBarElement = new HomeScreen(solo).actionBar();

        OfflineSettingsScreen offlineSettingsScreen = toolBarElement
                .clickSettingsOverflowButton()
                .clickOfflineSettings();
        assertThat(offlineSettingsScreen, is(visible()));

        waiter.waitTwoSeconds();

        startEventTracking();

        UpgradeScreen upgradeScreen = offlineSettingsScreen.clickSubscribe();
        assertThat(upgradeScreen, is(visible()));

        upgradeScreen.clickBuyForFailure();

        finishEventTracking(UPGRADE_TEST_SCENARIO);
    }

}
