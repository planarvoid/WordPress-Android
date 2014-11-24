package com.soundcloud.android.framework.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.with.With;

public class EmailOptInScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public EmailOptInScreen(Han solo) {
        super(solo);
        waiter.waitForElement(R.id.email_optin_body);
    }

    public HomeScreen clickNo() {
        String text = testDriver.getCurrentActivity().getString(R.string.optin_no);
        testDriver.findElement(With.text(text)).click();

        return new HomeScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
