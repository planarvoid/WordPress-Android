package com.soundcloud.android.tests.player;

import static android.content.Intent.ACTION_VIEW;
import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.HT_CREATOR_PROFILE_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Ignore;
import org.junit.Test;

import android.content.Intent;

public class UpgradeTest extends ActivityTest<MainActivity> {

    private static final String TRACKING_UPGRADE_SCENARIO = "specs/upgrade-from-player.spec";
    private ProfileScreen profileScreen;

    public UpgradeTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(ACTION_VIEW).setData(HT_CREATOR_PROFILE_URI));
        super.setUp();

        enableUpsell(getInstrumentation().getTargetContext());

        profileScreen = new ProfileScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    @Test
    @PaymentTest
    @Ignore
    public void testUserCanNavigateToSubscribePageFromPlayer() throws Exception {
        mrLocalLocal.startEventTracking();

        final VisualPlayerElement playerElement = profileScreen.playTrackWithTitle("HT 1");

        playerElement.waitForPlayState();
        playerElement.clickArtwork();

        UpgradeScreen upgradeScreen = playerElement.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        mrLocalLocal.verify(TRACKING_UPGRADE_SCENARIO);
    }
}
