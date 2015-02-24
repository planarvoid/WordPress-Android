package com.soundcloud.android.screens;

import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.framework.Han;

public class MyProfileScreen extends ProfileScreen {
    private static Class ACTIVITY = MeActivity.class;

    public MyProfileScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
