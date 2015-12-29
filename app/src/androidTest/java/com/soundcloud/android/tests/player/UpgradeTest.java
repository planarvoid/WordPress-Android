package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class UpgradeTest extends TrackingActivityTest<MainActivity> {

    private static final String TRACKING_UPGRADE_SCENARIO = "upgrade-from-player";

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

        final SearchResultsScreen searchResultsScreen = mainNavHelper.goToDiscovery()
                .clickSearch()
                .doSearch("BooHT3");

        startEventTracking();

        UpgradeScreen upgradeScreen = searchResultsScreen
                .clickFirstTrackItem()
                .clickUpgrade();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(TRACKING_UPGRADE_SCENARIO);
    }
}
