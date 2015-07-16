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
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeEnd2EndTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeEnd2EndTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.subscribeUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.PAYMENTS_TEST);
        super.setUp();
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
        settingsScreen = new StreamScreen(solo).actionBar().clickSettingsOverflowButton();
    }

    @PaymentTest
    public void testUserCanSubscribe() {
        PaymentStateHelper.resetTestAccount();
        UpgradeScreen upgradeScreen = settingsScreen
                .clickOfflineSettings()
                .clickSubscribe()
                .clickBuyForSuccess();
        waiter.waitTwoSeconds();
        BillingResponse.success().insertInto(solo.getCurrentActivity());
        waiter.waitFiveSeconds();
        assertTrue(upgradeScreen.isDisplayingSuccess());
    }

    @PaymentTest
    public void testInvalidPayment() {
        PaymentStateHelper.resetTestAccount();
        PaymentErrorScreen errorScreen = settingsScreen
                .clickOfflineSettings()
                .clickSubscribe()
                .clickBuyForFailure();
        waiter.waitTwoSeconds();
        BillingResponse.invalid().insertInto(solo.getCurrentActivity());
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_verification_issue));
    }

}
