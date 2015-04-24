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

    private ViewElement birthMonthText() {
        return testDriver.findElement(With.id(R.id.txt_choose_month));
    }

    private EditTextElement birthYearInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_enter_year)));
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

    public void clearEmail() {
        emailInputField().clearText();
    }

    public void typeEmail(String email) {
        emailInputField().typeText(email);
    }

    public void clearPassword() {
        passwordInputField().clearText();
    }

    public void typePassword(String password) {
        passwordInputField().typeText(password);
    }

    public void chooseBirthMonth(String month) {
        birthMonthText().click();
        testDriver.findElement(With.text(month)).click();
    }

    public void typeBirthYear(String year) {
        birthYearInputField().typeText(year);
    }

    public void clearBirthYear() {
        birthYearInputField().clearText();
    }

    public String getBirthYear() {
        return birthYearInputField().getText();
    }

    public void chooseGender(String genderChoice) {
        genderText().click();
        testDriver.findElement(With.text(genderChoice)).click();
    }

    public void typeCustomGender(String customGender) {
        customGenderInputField().typeText(customGender);
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

    public void closeSpamDialog() {
        final String dialogTitle = testDriver.getString(R.string.authentication_blocked_title);
        final ViewElement blockedDialog = testDriver.findElement(With.text(dialogTitle));
        assertTrue(blockedDialog.isVisible());
        testDriver.findElement(With.text(testDriver.getString(R.string.close))).click();
    }

    public void skipSignUpDetails() {
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
