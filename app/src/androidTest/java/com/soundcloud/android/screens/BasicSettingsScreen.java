package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.settings.SettingsActivity;

public class BasicSettingsScreen extends Screen {

    private static final Class ACTIVITY = SettingsActivity.class;

    public BasicSettingsScreen(Han solo) {
        super(solo);
    }

    public GoBackOnlineDialogElement goBackAndDisplayGoBackOnlineDialog() {
        testDriver.goBack();
        return new GoBackOnlineDialogElement(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
