package com.soundcloud.android.screens.auth.signup;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;

public class SignupSpamScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    private final Han solo;

    public SignupSpamScreen(Han solo) {
        super(solo);
        this.solo = solo;
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
