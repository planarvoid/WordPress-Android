package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.SuggestedUsersActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.view.View;
import android.widget.EditText;

public class SignUpScreen {

    private final Waiter waiter;
    private Han solo;

    public SignUpScreen(Han driver) {
        solo    = driver;
        waiter  = new Waiter(solo);
    }

    public void clickFacebookButton() {
        solo.clickOnView(R.id.facebook_btn);
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

    public void skipInfo(){
        solo.assertText(R.string.authentication_add_info_msg);
        solo.clickOnButtonResId(R.string.btn_skip);
    }

    public void waitForSuggestedUsers(){
        solo.waitForActivity(SuggestedUsersActivity.class);
        waiter.waitForListContent();
    }
}
