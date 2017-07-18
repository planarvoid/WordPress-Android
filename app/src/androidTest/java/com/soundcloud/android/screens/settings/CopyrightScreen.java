package com.soundcloud.android.screens.settings;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.settings.LicensesActivity;

public class CopyrightScreen extends Screen {

    private static final Class ACTIVITY = LicensesActivity.class;

    public CopyrightScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
