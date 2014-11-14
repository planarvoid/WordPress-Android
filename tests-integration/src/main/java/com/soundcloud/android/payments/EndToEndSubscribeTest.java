package com.soundcloud.android.payments;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PaymentScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class EndToEndSubscribeTest extends ActivityTestCase<MainActivity> {

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
        BillingResponses.ok(solo.getCurrentActivity());
        waiter.expectToast().toHaveText("Verifying");
    }

}
