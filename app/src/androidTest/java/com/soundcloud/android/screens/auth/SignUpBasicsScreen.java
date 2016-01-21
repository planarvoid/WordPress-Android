package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.SignUpSpamDialogElement;

public class SignUpBasicsScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    public SignUpBasicsScreen(Han testDriver) {
        super(testDriver);
    }

    private EditTextElement emailInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.auto_txt_email_address)));
    }

    private EditTextElement passwordInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.txt_choose_a_password)));
    }

    private EditTextElement ageInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.txt_enter_age)));
    }

    private ViewElement genderText() {
        return testDriver.findOnScreenElement(With.id(R.id.txt_choose_gender));
    }

    private EditTextElement customGenderInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.txt_enter_custom_gender)));
    }

    private ViewElement cancelButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_cancel));
    }

    private ViewElement doneButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_signup));
    }

    public ViewElement saveButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_save));
    }

    public ViewElement acceptTermsButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_accept_terms));
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
        testDriver.findOnScreenElement(With.text(genderChoice)).click();
        return this;
    }

    public SignUpBasicsScreen typeCustomGender(String customGender) {
        customGenderInputField().typeText(customGender);
        return this;
    }

    public boolean isDoneButtonEnabled() {
        testDriver.sleep(1);
        return doneButton().isEnabled();
    }

    public SignUpBasicsScreen signup() {
        doneButton().click();
        waiter.waitForElement(R.id.btn_accept_terms);
        return this;
    }

    public SignUpBasicsScreen acceptTerms() {
        acceptTermsButton().click();
        waiter.waitForElement(R.id.btn_save);
        return this;
    }

    public SignUpSpamDialogElement clickAcceptTermsOpensSpamDialog() {
        acceptTermsButton().click();
        return new SignUpSpamDialogElement(testDriver);
    }

    public EmailOptInScreen saveSignUpDetails() {
        saveButton().click();
        return new EmailOptInScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
