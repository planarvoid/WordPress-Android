package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SnippetsInStreamTest extends ActivityTest<MainActivity> {

    public SnippetsInStreamTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.freeMonetizedUserStream.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.PAYMENTS_TEST);
        super.setUp();
    }

    @Override
    protected void beforeStartActivity() {
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
        assertThat(mainNavHelper
                .goToStream()
                .scrollToUpsell()
                .clickUpgrade(), is(visible()));
    }
}
