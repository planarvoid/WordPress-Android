package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class OnboardScreen {

    private Han solo;
    private Waiter waiter;

    public OnboardScreen(Han driver) {
        solo    = driver;
        waiter  = new Waiter(solo);
    }

    public void clickSignUpButton() {
        solo.clickOnView(R.id.signup_btn);
        solo.waitForViewId(R.id.btn_signup, 5000);
    }

    public void clickLogInButton() {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        solo.waitForViewId(R.id.btn_login, 5000);
    }
}
