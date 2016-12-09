package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class UpgradeAudioTest extends TrackingActivityTest<MainActivity> {

    private static final String TEST_SCENARIO_UPGRADE_AUDIO = "audio-events-v1-profile-upgrade";
    private ProfileScreen profileScreen;

    public UpgradeAudioTest() {
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
    public void ignoretestUpgradeAudio() {
        // ignoring while we use progressive download, when we use HLS it should get rid of phantom HLS

        startEventTracking();

        final VisualPlayerElement visualPlayerElement = profileScreen.playTrackWithTitle("HT 1");

        visualPlayerElement.waitForPlayState();
        visualPlayerElement.clickArtwork();

        finishEventTracking(TEST_SCENARIO_UPGRADE_AUDIO);
    }
}
