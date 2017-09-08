package com.soundcloud.android.tests.payments;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.WebCheckoutScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class WebPaymentSmokeTest extends ActivityTest<MainActivity> {

    public WebPaymentSmokeTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableUpsell(getInstrumentation().getTargetContext());
    }

    @Test
    public void testOpensWebCheckoutScreen() throws Exception {
        WebCheckoutScreen checkoutScreen = mainNavHelper.goToMore()
                                                        .clickSubscribe()
                                                        .clickDefaultCheckout();

        assertThat(checkoutScreen, is(visible()));
    }

    @Test
    public void testOpensWebCheckoutViaProductChoiceScreen() throws Exception {
        WebCheckoutScreen checkoutScreen = mainNavHelper.goToMore()
                                                        .clickSubscribe()
                                                        .clickProductChoice()
                                                        .swipeToHighTierPlan()
                                                        .clickBuyButton();

        assertThat(checkoutScreen, is(visible()));
    }

}
