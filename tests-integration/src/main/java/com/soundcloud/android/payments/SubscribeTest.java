package com.soundcloud.android.payments;

import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PaymentScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class SubscribeTest extends ActivityTestCase<MainActivity> {

    private SettingsScreen settingsScreen;

    public SubscribeTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.PAYMENTS_TEST);
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
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
        BillingResponses.cancel(solo.getCurrentActivity());
        waiter.expectToast().toHaveText("User cancelled");
    }

}
