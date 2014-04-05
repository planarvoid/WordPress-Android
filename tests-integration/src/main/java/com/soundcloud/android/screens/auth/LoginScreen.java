package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

import android.R.id;
import android.widget.EditText;

public class LoginScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;

    public LoginScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
    }

    public EditText email() {
        return (EditText) solo.getView(R.id.auto_txt_email_address);
    }

    public EditText password() {
        return (EditText) solo.getView(R.id.txt_password);
    }

    public void typeUsername(String username) {
        solo.enterText(email(), username );
    }

    public void typePassword(String password) {
        solo.enterText(password(), password);
    }

    // What would be the best way of getting reference to element?
    public int logInButton() {
        return (R.string.authentication_log_in);
    }

    public void clickOkButton() {
        solo.clickOnOK();
    }

    public void clickOnFBSignInButton() {
        solo.clickOnText(R.string.authentication_log_in_with_facebook);
    }

    public void clickSignInWithGoogleButton() {
        solo.clickOnButton(R.string.authentication_log_in_with_google);
    }

    public RecoverPasswordScreen clickForgotPassword() {
        waiter.waitForElement(R.id.txt_i_forgot_my_password);
        solo.clickOnView(R.id.txt_i_forgot_my_password);
        return new RecoverPasswordScreen(solo);
    }

    public void selectUserFromDialog(String username) {
        solo.clickOnText(username);
        waiter.waitForActivity(OnboardActivity.class);
    }

    public void clickOnCancelButton() {
        solo.clickOnButton(R.string.cancel);
        solo.waitForActivity(OnboardActivity.class);
        solo.waitForViewId(R.id.tour_bottom_bar, 5000);
    }

    public void clickOnContinueButton() {
        solo.clickOnButton(R.string.btn_continue);
        waiter.waitForTextToDisappear("Logging you in");
    }
    public MainScreen loginAs(String username, String password) {
        solo.clearEditText(email());
        typeUsername(username);
        typePassword(password);
        solo.clickOnDone();
        return new MainScreen(solo);
    }

    public Screen loginAs(String username, String password, boolean validCredentials) {
        solo.clearEditText(email());

        typeUsername(username);
        typePassword(password);

        solo.clickOnDone();
        if (validCredentials) {
            return new MainScreen(solo);
        }
        return this;
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
