package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class SignUpScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    public SignUpScreen(Han testDriver) {
        super(testDriver);
    }

    private ViewElement googleSignInButton() {
        return testDriver.findElement(With.id(R.id.google_plus_btn));
    }

    private ViewElement facebookSignInButton() {
        return testDriver.findElement(With.id(R.id.facebook_btn));
    }

    private EditTextElement emailInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.auto_txt_email_address)));
    }

    private EditTextElement passwordInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_choose_a_password)));
    }

    private ViewElement cancelButton() {
        return testDriver.findElement(With.id(R.id.btn_cancel));
    }

    private ViewElement doneButton() {
        return testDriver.findElement(With.id(R.id.btn_signup));
    }

    private ViewElement skipButton() {
        return testDriver.findElement(With.id(R.id.btn_skip));
    }

    private ViewElement acceptTermsButton() {
        return testDriver.findElement(With.id(R.id.btn_accept_terms));
    }

    public void clickFacebookButton() {
        facebookSignInButton().click();
    }

    public void typeEmail(String email) {
        emailInputField().typeText(email);
    }

    public void typePassword(String password) {
        passwordInputField().typeText(password);
    }

    public boolean isDoneButtonEnabled() {
        return doneButton().isEnabled();
    }

    public void signup() {
        doneButton().click();
    }

    public void acceptTerms() {
        acceptTermsButton().click();
        waiter.waitForElement(R.id.btn_skip);
    }

    public void skipInfo() {
        skipButton().click();
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
