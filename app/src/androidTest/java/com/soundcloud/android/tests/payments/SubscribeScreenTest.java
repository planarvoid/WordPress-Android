package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.SubscribeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribeScreenTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeScreenTest() {
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
    public void testUserCanNavigateToSubscribePage() {
        SubscribeScreen subscribeScreen = settingsScreen
                .clickOfflineSettings()
                .clickSubscribe();
        assertThat(subscribeScreen, is(visible()));
    }

    @PaymentTest
    public void testUserIsPresentedSubscribeOption() {
        PaymentStateHelper.resetTestAccount();
        settingsScreen
                .clickOfflineSettings()
                .clickSubscribe()
                .clickBuy();
        waiter.waitTwoSeconds();
        BillingResponse.cancelled().insertInto(solo.getCurrentActivity());
        assertTrue(waiter.expectToastWithText(toastObserver, "User cancelled"));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

}
