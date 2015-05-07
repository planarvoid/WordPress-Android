package com.soundcloud.android.tests.auth.login;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.auth.LoginScreen;

public class LoginErrorScreen extends Screen {

    public LoginErrorScreen(Han solo) {
        super(solo);
        waiter.waitForElement(With.text(solo.getString(R.string.authentication_error_title)));
    }

    public String errorMessage() {
        return new TextElement(testDriver.findElement(With.id(android.R.id.message))).getText();
    }

    public LoginScreen clickOk() {
        okButton().click();
        return new LoginScreen(testDriver);
    }

    private ViewElement okButton() {
        final String okText = testDriver.getString(android.R.string.ok);
        return testDriver.findElement(With.text(okText));
    }

    @Override
    protected Class getActivity() {
        return OnboardActivity.class;
    }
}
