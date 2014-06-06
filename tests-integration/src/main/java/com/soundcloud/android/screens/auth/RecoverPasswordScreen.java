package com.soundcloud.android.screens.auth;

import android.R.id;
import android.widget.EditText;
import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;

public class RecoverPasswordScreen extends Screen {
    private static final Class ACTIVITY = RecoverActivity.class;


    public RecoverPasswordScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
    }

    private ViewElement emailInputField() {
        return testDriver.findElement(R.id.txt_email_address);
    }

    public void typeEmail(String text) {
        emailInputField().typeText(text);
    }

    public void clickOkButton () {
        testDriver.clickOnOK();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
