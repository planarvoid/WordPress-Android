package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class SnippetsInStreamTest extends TrackingActivityTest<MainActivity> {

    private static final String STREAM_UPSELL_TRACKING_TEST = "stream_upsell_tracking_test";

    public SnippetsInStreamTest() {
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
    }

    @Override
    protected void beforeStartActivity() {
        getWaiter().waitFiveSeconds();
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @PaymentTest
    public void testUserCanNavigateToSubscribePageFromPlayer() {
        VisualPlayerElement player = mainNavHelper
                .goToStream()
                .scrollToFirstSnippedTrack()
                .click();

        assertThat(player.clickUpgrade(), is(visible()));
    }

    @PaymentTest
    public void testUserCanNavigateToSubscribePageFromUpsell() {
        final StreamScreen streamScreen = mainNavHelper.goToStream();

        // this is here because we dont want to validate promoted and/or facebook invites impressions
        streamScreen.scrollToFirstNotPromotedTrackCard();

        startEventTracking();

        assertThat(streamScreen
                .scrollToUpsell()
                .clickUpgrade(), is(visible()));

        finishEventTracking(STREAM_UPSELL_TRACKING_TEST);
    }
}
