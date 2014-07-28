package com.soundcloud.android.screens;

import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.tests.Han;

public class ActivitiesScreen extends Screen {

    public ActivitiesScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ActivitiesActivity.class;
    }
}
