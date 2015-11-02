package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.settings.SettingsActivity;

public class BasicSettingsScreen extends Screen {

    private static final Class ACTIVITY = SettingsActivity.class;

    public BasicSettingsScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
