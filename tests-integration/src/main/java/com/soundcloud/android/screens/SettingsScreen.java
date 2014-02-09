package com.soundcloud.android.screens;

import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.tests.Han;

public class SettingsScreen extends Screen {
    private Class ACTIVITY = SettingsActivity.class;

    public SettingsScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}