package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;

public class SignUpScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    public SignUpScreen(Han testDriver) {
        super(testDriver);
    }

    private ViewElement googleSignInButton() {
        return testDriver.findElement(R.id.google_plus_btn);
    }

    private ViewElement facebookSignInButton() {
        return testDriver.findElement(R.id.facebook_btn);
    }

    private ViewElement emailInputField() {
        return testDriver.findElement(R.id.auto_txt_email_address);
    }

    private ViewElement passwordInputfield() {
        return testDriver.findElement(R.id.txt_choose_a_password);
    }

    private ViewElement cancelButton() {
        return testDriver.findElement(R.id.btn_cancel);
    }

    private ViewElement doneButton() {
        return testDriver.findElement(R.id.btn_signup);
    }

    private ViewElement acceptTermsButton() {
        return testDriver.findElement(R.id.btn_accept_terms);
    }

    public void clickFacebookButton() {
        facebookSignInButton().click();
    }

    public void typeEmail(String email) {
        emailInputField().typeText(email);
    }

    public void typePassword(String password) {
        passwordInputfield().typeText(password);
    }

    public boolean isDoneButtonEnabled() {
        return doneButton().isEnabled();
    }

    public void signup() {
        doneButton().click();
    }

    public void acceptTerms() {
        acceptTermsButton().click();
        waiter.waitForLogInDialog();
    }

    public void skipInfo() {
        testDriver.assertText(R.string.authentication_add_info_msg);
        testDriver.clickOnButtonResId(R.string.btn_skip);
    }

    public SuggestedUsersScreen waitForSuggestedUsers() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new SuggestedUsersScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
