package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.EditText;

public class SignUpScreen {

    private Han solo;

    public SignUpScreen(Han driver) {
        solo    = driver;
    }

    public EditText email() {
        return (EditText) solo.getView(R.id.auto_txt_email_address);
    }

    public EditText password() {
        return (EditText) solo.getView(R.id.txt_choose_a_password);
    }

    public void typeEmail(String email) {
        solo.enterText(email(), email );
    }

    public void typePassword(String password) {
        solo.enterText(password(), password);
    }

    public View getDoneButton(){
        return solo.getView(R.id.btn_signup);
    }

    public void signup(){
        solo.clickOnView(R.id.btn_signup);
        solo.waitForViewId(R.id.btn_accept_terms, 1000);
    }

    public void acceptTerms(){
        solo.clickOnView(R.id.btn_accept_terms);
    }


}
