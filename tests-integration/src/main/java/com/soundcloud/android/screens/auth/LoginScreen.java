package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.viewelements.EditTextElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.viewelements.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.R.id;

public class LoginScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;

    public LoginScreen(Han testDriver) {
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
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_password)));
    }

    private ViewElement loginButton() {
        return testDriver.findElement(With.id(R.id.btn_login));
    }

    private ViewElement forgotPasswordButton() {
        return testDriver.findElement(With.id(R.id.txt_i_forgot_my_password));
    }

    public void clickOkButton() {
        testDriver.clickOnText(android.R.string.ok);
    }

    public void clickOnFBSignInButton() {
        facebookSignInButton().click();
    }

    public void clickSignInWithGoogleButton() {
        googleSignInButton().click();
    }

    public RecoverPasswordScreen clickForgotPassword() {
        forgotPasswordButton().click();
        return new RecoverPasswordScreen(testDriver);
    }

    public void selectUserFromDialog(String username) {
        testDriver.findElement(With.text(username)).click();
        waiter.waitForActivity(OnboardActivity.class);
    }

    public void clickOnContinueButton() {
        testDriver.clickOnButton(R.string.btn_continue);
        waiter.waitForTextToDisappear("Logging you in");
    }
    public MainScreen loginAs(String username, String password) {
        emailInputField().clearText();

        passwordInputField().typeText(password);
        emailInputField().typeText(username);
        loginButton().click();
        return new MainScreen(testDriver);
    }

    public Screen loginAs(String username, String password, boolean validCredentials) {
        emailInputField().clearText();

        emailInputField().typeText(username);
        passwordInputField().typeText(password);

        loginButton().click();
        if (validCredentials) {
            return new MainScreen(testDriver);
        }
        return this;
    }

    @Override
    public boolean isVisible() {
        return loginButton().isVisible();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
