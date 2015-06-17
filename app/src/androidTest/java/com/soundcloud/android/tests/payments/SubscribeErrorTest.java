package com.soundcloud.android.tests.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PaymentErrorScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeErrorTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeErrorTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.subscribeUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Flag.PAYMENTS_TEST);
        super.setUp();
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
        settingsScreen = new StreamScreen(solo).actionBar().clickSettingsOverflowButton();
    }

    @PaymentTest
    public void testAlreadySubscribedError() {
        PaymentStateHelper.resetTestAccount();
        subscribe();
        solo.goBack();
        PaymentErrorScreen errorScreen = settingsScreen
                .clickOfflineSettings()
                .clickSubscribe()
                .clickBuyForFailure();
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_already_subscribed));
    }

    private void subscribe() {
        settingsScreen
                .clickOfflineSettings()
                .clickSubscribe()
                .clickBuyForSuccess();
        waiter.waitTwoSeconds();
        BillingResponse.success().insertInto(solo.getCurrentActivity());
        waiter.waitFiveSeconds();
    }

}
