package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.auth.LoginScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class HomeScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;

    public HomeScreen(Han solo) {
        super(solo);
    }

    public LoginScreen clickLogInButton() {
        logInButton().click();
        return new LoginScreen(testDriver);
    }

    public SignUpScreen clickSignUpButton() {
        signUpButton().click();
        return new SignUpScreen(testDriver);
    }

    public boolean hasItemByUsername(String username){
        return !testDriver.findElements(With.text(username)).isEmpty();
    }

    private ViewElement bottomBar() {
        return testDriver.findElement(With.id(R.id.tour_bottom_bar));
    }

    private ViewElement signUpButton() {
        return bottomBar().findElement(With.id(R.id.signup_btn));
    }

    private ViewElement logInButton() {
       return bottomBar().findElement(With.id(R.id.login_btn));
    }

    @Override
    public boolean isVisible() {
        return bottomBar().isVisible();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
