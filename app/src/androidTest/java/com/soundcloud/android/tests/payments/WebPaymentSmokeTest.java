package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.WebCheckoutScreen;
import com.soundcloud.android.tests.ActivityTest;

public class WebPaymentSmokeTest extends ActivityTest<MainActivity> {

    public WebPaymentSmokeTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    public void testOpensWebCheckoutScreen() {
        WebCheckoutScreen checkoutScreen = mainNavHelper.goToMore()
                .clickSubscribe()
                .clickDefaultCheckout();

        assertThat(checkoutScreen, is(visible()));
    }

    public void testOpensWebCheckoutViaProductChoiceScreen() {
        WebCheckoutScreen checkoutScreen = mainNavHelper.goToMore()
                .clickSubscribe()
                .clickProductChoice()
                .swipeToHighTierPlan()
                .clickBuyButton();

        assertThat(checkoutScreen, is(visible()));
    }

}
