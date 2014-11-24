package com.soundcloud.android.framework.screens.auth.signup;

import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.framework.screens.Screen;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class SignupSpamScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    private final Han solo;

    public SignupSpamScreen(Han solo) {
        super(solo);
        this.solo = solo;
    }

    public void clickTryAgain() {
        testDriver.clickOnText(com.soundcloud.android.R.string.try_again);
    }

    @Override
    public boolean isVisible() {
        return solo.findElement(With.text(solo.getString(com.soundcloud.android.R.string.authentication_captcha_message))).isVisible();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
