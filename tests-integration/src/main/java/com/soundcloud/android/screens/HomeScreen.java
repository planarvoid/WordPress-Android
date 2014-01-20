package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.R.string;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.auth.LoginScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.Han;

import android.R.id;

public class HomeScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    private static final int CONTENT_ROOT = id.content;

    public HomeScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(CONTENT_ROOT);
    }

    public LoginScreen clickLogInButton() {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        waiter.waitForText(solo.getString(string.done));
        return new LoginScreen(solo);
    }

    public SignUpScreen clickSignUpButton() {
        solo.clickOnView(R.id.signup_btn);
        solo.waitForViewId(R.id.btn_signup, 5000);
        return new SignUpScreen(solo);
    }

    public boolean hasItemByUsername(String username){
        return solo.searchText(username, true);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
