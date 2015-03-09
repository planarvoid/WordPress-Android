package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.payments.SubscribeSuccessActivity;

public class SubscribeSuccessScreen extends Screen {

    private static final Class ACTIVITY = SubscribeSuccessActivity.class;

    public SubscribeSuccessScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SettingsScreen goBack() {
        testDriver.goBack();
        return new SettingsScreen(testDriver);
    }

}
