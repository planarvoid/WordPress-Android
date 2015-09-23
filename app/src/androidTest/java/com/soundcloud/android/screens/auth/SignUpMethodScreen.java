package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;

public class SignUpMethodScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    public SignUpMethodScreen(Han testDriver) {
        super(testDriver);
    }

    private ViewElement googleSignInButton() {
        return testDriver.findElement(With.id(R.id.google_plus_btn));
    }

    private ViewElement facebookSignInButton() {
        return testDriver.findElement(With.id(R.id.facebook_btn));
    }

    private ViewElement emailSignInButton() {
        return testDriver.findElement(With.id(R.id.signup_with_email));
    }

    public void clickFacebookButton() {
        facebookSignInButton().click();
    }

    public void acceptTerms() {
        acceptTermsButton().click();
        waiter.waitForElement(R.id.btn_skip);
    }

    private ViewElement acceptTermsButton() {
        return testDriver.findElement(With.id(R.id.btn_accept_terms));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SignUpBasicsScreen clickByEmailButton() {
        emailSignInButton().click();
        return new SignUpBasicsScreen(testDriver);
    }
}
