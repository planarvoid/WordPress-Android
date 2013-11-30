package com.soundcloud.android.screens.auth;

import android.R.id;
import android.widget.EditText;
import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

public class RecoverPasswordScreen extends Screen {
    private static final Class ACTIVITY = RecoverActivity.class;


    public RecoverPasswordScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
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

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
