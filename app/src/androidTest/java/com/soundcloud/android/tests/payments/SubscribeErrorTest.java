package com.soundcloud.android.tests.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PaymentErrorScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.SubscribeScreen;
import com.soundcloud.android.screens.SubscribeSuccessScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeErrorTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeErrorTest() {
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
    public void testAlreadySubscribedError() {
        PaymentStateHelper.resetTestAccount();
        SubscribeSuccessScreen successScreen = subscribe();
        assertTrue(successScreen.isVisible());
        successScreen.goBack();
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        PaymentErrorScreen errorScreen = subscribeScreen.clickBuyForFailure();
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_already_subscribed));
    }

    @PaymentTest
    public void testUnconfirmedEmailError() {
        // TODO
    }

    @PaymentTest
    public void testWrongUserError() {
        // TODO
    }

    @PaymentTest
    public void testStaleCheckoutError() {
        // TODO
    }

    private SubscribeSuccessScreen subscribe() {
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        SubscribeSuccessScreen successScreen = subscribeScreen.clickBuyForSuccess();
        waiter.waitTwoSeconds();
        BillingResponse.success().insertInto(solo.getCurrentActivity());
        waiter.waitFiveSeconds();
        return successScreen;
    }

}
