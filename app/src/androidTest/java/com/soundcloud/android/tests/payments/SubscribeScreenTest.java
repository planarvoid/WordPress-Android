package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.SubscribeScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

public class SubscribeScreenTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeScreenTest() {
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
    public void testUserCanNavigateToSubscribePage() {
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        assertThat(subscribeScreen, is(visible()));
    }

    @PaymentTest
    public void testUserIsPresentedSubscribeOption() {
        SubscribeScreen subscribeScreen = settingsScreen.clickSubscribe();
        subscribeScreen.clickBuy();
        waiter.waitTwoSeconds();
        BillingResponse.cancelled().insertInto(solo.getCurrentActivity());
        assertTrue(waiter.expectToastWithText(toastObserver, "User cancelled"));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
