package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.R.string;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.auth.LoginScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;

public class HomeScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;

    public HomeScreen(Han solo) {
        super(solo);
    }

    private ViewElement bottomBar() {
        return testDriver.findElement(R.id.tour_bottom_bar);
    }

    public LoginScreen clickLogInButton() {
        logInButton().click();
        waiter.waitForText(testDriver.getString(string.done));
        return new LoginScreen(testDriver);
    }

    public SignUpScreen clickSignUpButton() {
        testDriver.clickOnView(R.id.signup_btn);
        return new SignUpScreen(testDriver);
    }

    public boolean hasItemByUsername(String username){
        return testDriver.searchText(username, true);
    }

    private ViewElement logInButton() {
       return bottomBar().findElement(R.id.login_btn);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
