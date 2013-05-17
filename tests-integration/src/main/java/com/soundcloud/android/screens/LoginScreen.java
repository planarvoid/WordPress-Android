package com.soundcloud.android.screens;

import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.activity.auth.Recover;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.tests.Han;

import android.widget.EditText;

public class LoginScreen {

    private Han solo;

    public LoginScreen(Han driver) {
        solo = driver;
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

    public void clickLogInButton() {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        solo.waitForActivity(Onboard.class);
        solo.waitForViewId(R.id.btn_login, 5000);
    }

    public void clickOkButton() {
        solo.clickOnOK();
    }

    public void clickSignInWithGoogleButton() {
        solo.clickOnButton(R.string.authentication_log_in_with_google);
    }

    public void clickForgotPassword () {
        solo.clickOnView(R.id.txt_i_forgot_my_password);
        solo.waitForActivity(Recover.class);
    }

    public void selectUserFromDialog(String username) {
        solo.clickOnText(username);
        solo.waitForActivity(Onboard.class);
    }

    public void clickOnCancelButton() {
        solo.clickOnButton(R.string.cancel);
        solo.waitForActivity(Onboard.class);
        solo.waitForViewId(R.id.tour_bottom_bar, 5000);
    }
    public void clickOnContinueButton() {
        solo.clickOnButton(R.string.btn_continue);

        //TODO Move this and create a class responsible for waiting
        Condition condition = new Condition() {

            @Override
            public boolean isSatisfied() {
                return !solo._searchText("Logging you in", true);
            }
        };

        solo.waitForCondition(condition, 5000);
    }

    // Logs in
    public LoginScreen loginAs(String username, String password) {

        solo.clearEditText(email());
        typeUsername(username);
        typePassword(password);
        solo.clickOnDone();
        solo.waitForActivity(Home.class);
        solo.waitForViewId(R.id.title, 5000);
        return this;
    }

    public LoginScreen loginAs(String username, String password, boolean validCredentials) {
        solo.clearEditText(email());

        typeUsername(username);
        typePassword(password);

        solo.clickOnDone();
        if (validCredentials) {
            solo.waitForActivity(Home.class);
            solo.waitForView(solo.getView(R.id.title));
        }

        return this;
    }

}
