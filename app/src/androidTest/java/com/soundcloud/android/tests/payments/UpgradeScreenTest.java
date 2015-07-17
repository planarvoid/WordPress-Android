package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

public class UpgradeScreenTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public UpgradeScreenTest() {
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
    public void testUserCanNavigateToSubscribePage() {
        UpgradeScreen upgradeScreen = settingsScreen
                .clickOfflineSettings()
                .clickSubscribe();
        assertThat(upgradeScreen, is(visible()));
    }

    @PaymentTest
    public void testUserIsPresentedSubscribeOption() {
        PaymentStateHelper.resetTestAccount();
        PaymentErrorScreen errorScreen = settingsScreen
                .clickOfflineSettings()
                .clickSubscribe()
                .clickBuyForFailure();
        waiter.waitTwoSeconds();
        BillingResponse.cancelled().insertInto(solo.getCurrentActivity());
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_cancelled));
    }

}
