package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.widget.EditText;

public class RecoverPasswordScreen {
    private Han solo;

    public RecoverPasswordScreen(Han driver) {
        solo = driver;
    }

    public EditText email() {
        return (EditText) solo.getView(R.id.txt_email_address);
    }

    public void typeEmail(String email) {
        solo.enterText(email(), email);
    }

    public void clickOkButton () {
        solo.clickOnOK();
    }

}
