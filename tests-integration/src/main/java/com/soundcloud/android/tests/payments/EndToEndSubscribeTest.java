package com.soundcloud.android.tests.payments;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.SubscribeScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.SubscribeSuccessScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

import android.test.suitebuilder.annotation.Smoke;

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

    @Smoke
    public void testUserCanSubscribe() {
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        subscribeScreen.clickBuy();
        waiter.waitTwoSeconds();
        new BillingResponse(solo.getCurrentActivity()).forSuccess().insert();
        waiter.waitTwoSeconds();
        assertTrue(new SubscribeSuccessScreen(solo).isVisible());
    }

    @Smoke
    public void testInvalidPayment() {
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        subscribeScreen.clickBuy();
        waiter.waitTwoSeconds();
        new BillingResponse(solo.getCurrentActivity()).forInvalid().insert();
        waiter.waitTwoSeconds();
        assertTrue(waiter.expectToastWithText(toastObserver, "Verification failed"));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

}
