package com.soundcloud.android.screens.auth.signup;

import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

public class SignupEmailTakenScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    private final Han solo;

    public SignupEmailTakenScreen(Han solo) {
        super(solo);
        this.solo = solo;
    }

    @Override
    public boolean isVisible() {
        return solo.findElement(With.text(solo.getString(com.soundcloud.android.R.string.authentication_email_taken_message))).isVisible();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
