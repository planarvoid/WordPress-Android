package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

@EventTrackingTest
public class UpgradeAudioTest extends TrackingActivityTest<MainActivity> {

    private static final String TEST_SCENARIO_UPGRADE_AUDIO = "audio-events-v1-profile-upgrade";
    private ProfileScreen profileScreen;

    public UpgradeAudioTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
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
    public void testUpgradeAudio() {
        startEventTracking();

        final VisualPlayerElement visualPlayerElement = profileScreen.playTrackWithTitle("HT 1");

        visualPlayerElement.waitForPlayState();
        visualPlayerElement.clickArtwork();

        finishEventTracking(TEST_SCENARIO_UPGRADE_AUDIO);
    }
}
