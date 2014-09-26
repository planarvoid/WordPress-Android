package com.soundcloud.android.screens;

import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

public class SettingsScreen extends Screen {
    private static final Class ACTIVITY = SettingsActivity.class;

    public SettingsScreen(Han solo) {
        super(solo);
    }

    public PaymentScreen clickSubscribe() {
        testDriver.clickOnText(R.string.pref_subscription_buy_title);
        return new PaymentScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}