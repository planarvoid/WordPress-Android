package com.soundcloud.android.screens;

import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

public class EmailConfirmScreen extends Screen {

    private static final Class ACTIVITY = EmailConfirmationActivity.class;

    public EmailConfirmScreen(Han solo) {
        super(solo);
    }

    public EmailOptInScreen clickConfirmLater() {
        solo.clickOnText(R.string.email_confirmation_confirm_later);
        return new EmailOptInScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
