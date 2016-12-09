package com.soundcloud.android.tests.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.PaymentErrorScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeEnd2EndTest extends ActivityTest<MainActivity> {

    private OfflineSettingsScreen settingsScreen;

    public SubscribeEnd2EndTest() {
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
    public void testUserCanSubscribe() {
        PaymentStateHelper.resetTestAccount(getActivity());
        settingsScreen
                .clickSubscribe()
                .clickBuyForSuccess();
        waiter.waitTwoSeconds();
        BillingResponse.success().insertInto(solo.getCurrentActivity());

        // TODO: Is it possible to assert on Go Onboarding screen after app restart?
    }

    @PaymentTest
    public void testInvalidPayment() {
        PaymentStateHelper.resetTestAccount(getActivity());
        PaymentErrorScreen errorScreen = settingsScreen
                .clickSubscribe()
                .clickBuyForFailure();
        waiter.waitTwoSeconds();
        BillingResponse.invalid().insertInto(solo.getCurrentActivity());
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_verification_issue));
    }

}
