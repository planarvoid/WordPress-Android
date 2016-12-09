package com.soundcloud.android.tests.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.PaymentErrorScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeErrorTest extends ActivityTest<MainActivity> {

    private OfflineSettingsScreen settingsScreen;

    public SubscribeErrorTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.subscribeUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
        settingsScreen = mainNavHelper.goToOfflineSettings();
    }

    @PaymentTest
    public void testAlreadySubscribedError() {
        PaymentStateHelper.resetTestAccount(getActivity());
        subscribe();
        solo.goBack();
        PaymentErrorScreen errorScreen = settingsScreen
                .clickSubscribe()
                .clickBuyForFailure();
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_already_subscribed));
    }

    private void subscribe() {
        settingsScreen
                .clickSubscribe()
                .clickBuyForSuccess();
        waiter.waitTwoSeconds();
        BillingResponse.success().insertInto(solo.getCurrentActivity());
        waiter.waitFiveSeconds();
    }

}
