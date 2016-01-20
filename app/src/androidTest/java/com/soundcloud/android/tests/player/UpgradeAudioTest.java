package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
public class UpgradeAudioTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_UPGRADE_AUDIO = "audio-events-v1-search-upgrade";

    public UpgradeAudioTest() {
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
    public void testUpgradeAudio() {

        final SearchResultsScreen searchResultsScreen = mainNavHelper.goToDiscovery()
                .clickSearch()
                .doSearch("BooHT3");

        startEventTracking();

        final VisualPlayerElement visualPlayerElement = searchResultsScreen
                .clickFirstTrackItem();

        visualPlayerElement.waitForPlayState();
        visualPlayerElement.clickArtwork();

        finishEventTracking(TEST_SCENARIO_UPGRADE_AUDIO);
    }
}
