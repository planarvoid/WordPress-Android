package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.framework.screens.MainScreen;
import com.soundcloud.android.framework.screens.PaymentScreen;
import com.soundcloud.android.framework.screens.SettingsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

public class SubscribeTest extends ActivityTest<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.PAYMENTS_TEST);
        TestUser.subscribeUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        settingsScreen = new MainScreen(solo).actionBar().clickSettingsOverflowButton();
    }

    public void testUserCanNavigateToSubscribePage() {
        PaymentScreen paymentScreen = settingsScreen.clickSubscribe();
        assertThat(paymentScreen, is(visible()));
    }

    public void testUserIsPresentedSubscribeOption() {
        PaymentScreen paymentScreen = settingsScreen.clickSubscribe();
        paymentScreen.clickBuy();
        waiter.waitTwoSeconds();
        new BillingResponse(solo.getCurrentActivity()).forCancel().insert();
        waiter.expectToast().toHaveText("User cancelled");
    }

}
