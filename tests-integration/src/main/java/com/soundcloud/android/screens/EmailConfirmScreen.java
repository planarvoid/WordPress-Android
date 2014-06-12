package com.soundcloud.android.screens;

import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.with.With;

public class EmailConfirmScreen extends Screen {

    private static final Class ACTIVITY = EmailConfirmationActivity.class;

    public EmailConfirmScreen(Han solo) {
        super(solo);
    }

    public EmailOptInScreen clickConfirmLater() {
        testDriver.findElement(With.text(testDriver.getString(R.string.email_confirmation_confirm_later))).click();
        return new EmailOptInScreen(testDriver);
    }

    public HomeScreen goBack() {
        testDriver.goBack();
        return new HomeScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
