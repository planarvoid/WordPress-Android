package com.soundcloud.android.tests.payments;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PaymentScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

public class EndToEndSubscribeTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public EndToEndSubscribeTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.PAYMENTS_TEST);
        TestUser.subscribeUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        settingsScreen = new MainScreen(solo).actionBar().clickSettingsOverflowButton();
    }

    public void testUserCanSubscribe() {
        PaymentScreen paymentScreen = settingsScreen.clickSubscribe();
        paymentScreen.clickBuy();
        waiter.waitTwoSeconds();
        new BillingResponse(solo.getCurrentActivity()).forSuccess().insert();
        waiter.waitTwoSeconds();
        waiter.expectToast().toHaveText("Success");
    }

    public void testInvalidPayment() {
        PaymentScreen paymentScreen = settingsScreen.clickSubscribe();
        paymentScreen.clickBuy();
        waiter.waitTwoSeconds();
        new BillingResponse(solo.getCurrentActivity()).forInvalid().insert();
        waiter.waitTwoSeconds();
        waiter.expectToast().toHaveText("Verification failed");
    }

}
