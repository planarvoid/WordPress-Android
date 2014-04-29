package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

public class EmailOptInScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public EmailOptInScreen(Han solo) {
        super(solo);
        waiter.waitForElement(R.id.email_optin_body);
    }

    public HomeScreen clickNo() {
        solo.clickOnText(R.string.optin_no);
        return new HomeScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
