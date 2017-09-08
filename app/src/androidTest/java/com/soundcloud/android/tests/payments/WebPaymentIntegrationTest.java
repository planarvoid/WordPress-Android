package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.TestUser.subscribeUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Ignore;
import org.junit.Test;

public class WebPaymentIntegrationTest extends ActivityTest<MainActivity> {

    public WebPaymentIntegrationTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return subscribeUser;
    }

    @PaymentTest
    @Ignore
    @Test
    public void testEndToEndMidTierPurchase() throws Exception {
        CollectionScreen collectionScreen = mainNavHelper.goToMore()
                                                         .clickSubscribe()
                                                         .clickDefaultCheckout()
                                                         .populateTestCardData()
                                                         .subscribe()
                                                         .clickStartButton();

        assertThat(collectionScreen, is(visible()));
    }

    @Test
    @Ignore
    @PaymentTest
    public void testEndToEndHighTierPurchase() throws Exception {
        CollectionScreen collectionScreen = mainNavHelper.goToMore()
                                                         .clickSubscribe()
                                                         .clickProductChoice()
                                                         .swipeToHighTierPlan()
                                                         .clickBuyButton()
                                                         .populateTestCardData()
                                                         .subscribe()
                                                         .clickStartButton();

        assertThat(collectionScreen, is(visible()));
    }

}
