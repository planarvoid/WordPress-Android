package com.soundcloud.android.framework.screens;

import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.framework.Han;
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