package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.tests.ActivityTest;

public class WebPaymentIntegrationTest extends ActivityTest<MainActivity> {

    public WebPaymentIntegrationTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.subscribeUser;
    }

    @PaymentTest
    public void testEndToEndHighTierPlanPurchase()  {
        CollectionScreen collectionScreen = mainNavHelper.goToMore()
                                                         .clickSubscribe()
                                                         .clickDefaultCheckout()
                                                         .populateTestCardData()
                                                         .subscribe()
                                                         .clickStartButton();

        assertThat(collectionScreen, is(visible()));
    }

}
