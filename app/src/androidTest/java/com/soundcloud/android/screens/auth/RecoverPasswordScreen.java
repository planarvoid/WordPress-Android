package com.soundcloud.android.screens.auth;

import android.R.id;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class RecoverPasswordScreen extends Screen {
    private static final Class ACTIVITY = RecoverActivity.class;


    public RecoverPasswordScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
    }

    private EditTextElement emailInputField() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.txt_email_address)));
    }

    public RecoverPasswordScreen typeEmail(String text) {
        emailInputField().typeText(text);
        return this;
    }

    public void clickOkButton () {
        testDriver.clickOnText(android.R.string.ok);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
