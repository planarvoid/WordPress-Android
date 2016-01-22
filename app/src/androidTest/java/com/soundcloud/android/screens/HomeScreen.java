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
        return testDriver.findOnScreenElement(With.id(R.id.tour_bottom_bar));
    }

    private ViewElement signUpButton() {
        return bottomBar().findOnScreenElement(With.id(R.id.btn_create_account));
    }

    private ViewElement logInButton() {
       return bottomBar().findOnScreenElement(With.id(R.id.btn_login));
    }

    @Override
    public boolean isVisible() {
        return bottomBar().isOnScreen();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
