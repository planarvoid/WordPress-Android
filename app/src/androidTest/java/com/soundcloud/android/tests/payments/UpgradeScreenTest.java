package com.soundcloud.android.tests.payments;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PaymentTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.PaymentErrorScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class UpgradeScreenTest extends ActivityTest<MainActivity> {

    private MoreScreen moreScreen;

    public UpgradeScreenTest() {
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
        moreScreen = mainNavHelper.goToMore();
    }

    @PaymentTest
    public void testUserCanNavigateToSubscribePage() {
        UpgradeScreen upgradeScreen = moreScreen
                .clickSubscribe();
        assertThat(upgradeScreen, is(visible()));
    }

    @PaymentTest
    public void testUserIsPresentedSubscribeOption() {
        PaymentStateHelper.resetTestAccount(getActivity());
        PaymentErrorScreen errorScreen = moreScreen
                .clickSubscribe()
                .clickBuyForFailure();
        waiter.waitTwoSeconds();
        BillingResponse.cancelled().insertInto(solo.getCurrentActivity());
        errorScreen.waitForDialog();
        assertEquals(errorScreen.getMessage(), solo.getString(R.string.payments_error_cancelled));
    }

}
