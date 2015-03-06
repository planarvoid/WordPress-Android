package com.soundcloud.android.tests.payments;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.SubscribeScreen;
import com.soundcloud.android.screens.SubscribeSuccessScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeEnd2EndTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeEnd2EndTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Flag.PAYMENTS_TEST);
        TestUser.subscribeUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        settingsScreen = new MainScreen(solo).actionBar().clickSettingsOverflowButton();
    }

    @PaymentTest
    public void testUserCanSubscribe() {
        PaymentStateHelper.resetTestAccount();
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        subscribeScreen.clickBuy();
        waiter.waitTwoSeconds();
        BillingResponse.success().insertInto(solo.getCurrentActivity());
        waiter.waitTwoSeconds();
        assertTrue(new SubscribeSuccessScreen(solo).isVisible());
    }

    @PaymentTest
    public void testInvalidPayment() {
        PaymentStateHelper.resetTestAccount();
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        subscribeScreen.clickBuy();
        waiter.waitTwoSeconds();
        BillingResponse.invalid().insertInto(solo.getCurrentActivity());
        waiter.waitTwoSeconds();
        assertTrue(waiter.expectToastWithText(toastObserver, "Verification failed"));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

}
