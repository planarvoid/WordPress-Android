package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class UpgradeTest extends TrackingActivityTest<MainActivity> {

    private static final String TRACKING_UPGRADE_SCENARIO = "upgrade-from-player";
    private ProfileScreen profileScreen;

    public UpgradeTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.HT_CREATOR_PROFILE_URI));
        super.setUp();

        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());

        profileScreen = new ProfileScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    @PaymentTest
    public void testUserCanNavigateToSubscribePageFromPlayer() {
        startEventTracking();

        final VisualPlayerElement playerElement = profileScreen.playTrackWithTitle("HT 1");

        playerElement.waitForPlayState();
        playerElement.clickArtwork();

        UpgradeScreen upgradeScreen = playerElement.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(TRACKING_UPGRADE_SCENARIO);
    }
}
