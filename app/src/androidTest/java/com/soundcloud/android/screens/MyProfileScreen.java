package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.ProfileActivity;

public class MyProfileScreen extends ProfileScreen {
    private static Class ACTIVITY = ProfileActivity.class;

    public MyProfileScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
