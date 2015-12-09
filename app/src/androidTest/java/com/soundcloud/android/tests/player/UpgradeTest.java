package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class UpgradeTest extends ActivityTest<MainActivity> {

    public UpgradeTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.freeMonetizedUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.PAYMENTS_TEST);
        super.setUp();
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @PaymentTest
    public void testUserCanNavigateToSubscribePageFromPlayer() {
        UpgradeScreen upgradeScreen = mainNavHelper.goToDiscovery()
                .clickSearch()
                .doSearch("BooHT3")
                .clickFirstTrackItem()
                .clickUpgrade();

        assertThat(upgradeScreen, is(visible()));
    }
}
