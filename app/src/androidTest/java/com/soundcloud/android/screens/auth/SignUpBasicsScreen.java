package com.soundcloud.android.screens.auth;

import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;

public class SignUpBasicsScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    public SignUpBasicsScreen(Han testDriver) {
        super(testDriver);
    }

    private EditTextElement emailInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.auto_txt_email_address)));
    }

    private EditTextElement passwordInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_choose_a_password)));
    }

    private EditTextElement ageInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_enter_age)));
    }

    private ViewElement genderText() {
        return testDriver.findElement(With.id(R.id.txt_choose_gender));
    }

    private EditTextElement customGenderInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_enter_custom_gender)));
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

    public SignUpBasicsScreen clearEmail() {
        emailInputField().clearText();
        return this;
    }

    public SignUpBasicsScreen typeEmail(String email) {
        emailInputField().clearText();
        emailInputField().typeText(email);
        return this;
    }

    public SignUpBasicsScreen clearPassword() {
        passwordInputField().clearText();
        return this;
    }

    public SignUpBasicsScreen typePassword(String password) {
        passwordInputField().typeText(password);
        return this;
    }

    public SignUpBasicsScreen typeAge(int age) {
        ageInputField().typeText(String.valueOf(age));
        return this;
    }

    public void clearAge() {
        ageInputField().clearText();
    }

    public SignUpBasicsScreen chooseGender(String genderChoice) {
        genderText().click();
        testDriver.findElement(With.text(genderChoice)).click();
        return this;
    }

    public SignUpBasicsScreen typeCustomGender(String customGender) {
        customGenderInputField().typeText(customGender);
        return this;
    }

    public boolean isDoneButtonEnabled() {
        return doneButton().isEnabled();
    }

    public SignUpBasicsScreen signup() {
        doneButton().click();
        return this;
    }

    public SignUpBasicsScreen acceptTerms() {
        acceptTermsButton().click();
        waiter.waitForElement(R.id.btn_skip);
        return this;
    }

    public void closeSpamDialog() {
        final String dialogTitle = testDriver.getString(R.string.authentication_blocked_title);
        final ViewElement blockedDialog = testDriver.findElement(With.text(dialogTitle));
        assertTrue(blockedDialog.isVisible());
        testDriver.findElement(With.text(testDriver.getString(R.string.contact_support))).click();
    }

    public SignUpBasicsScreen skipSignUpDetails() {
        skipButton().click();
        return this;
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
