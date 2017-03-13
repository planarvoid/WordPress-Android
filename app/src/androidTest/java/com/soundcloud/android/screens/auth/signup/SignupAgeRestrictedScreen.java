package com.soundcloud.android.screens.auth.signup;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;

public class SignupAgeRestrictedScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;
    private final Han solo;
    private final int minimumAge;

    public SignupAgeRestrictedScreen(Han solo, int minimumAge) {
        super(solo);
        this.solo = solo;
        this.minimumAge = minimumAge;
    }

    @Override
    public boolean isVisible() {
        return solo.findOnScreenElement(With.text(solo.getString(R.string.authentication_age_restriction, minimumAge)))
                   .isOnScreen();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
