package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.auth.LoginScreen;
import com.soundcloud.android.screens.auth.SignUpMethodScreen;

public class HomeScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;

    public HomeScreen(Han solo) {
        super(solo);
    }

    public LoginScreen clickLogInButton() {
        logInButton().click();
        return new LoginScreen(testDriver);
    }

    public SignUpMethodScreen clickSignUpButton() {
        signUpButton().click();
        return new SignUpMethodScreen(testDriver);
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
