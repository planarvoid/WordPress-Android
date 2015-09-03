package com.soundcloud.android.screens;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.ProfileActivity;

public class MyProfileScreen extends ProfileScreen {

    public MyProfileScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return BuildConfig.FEATURE_NEW_PROFILE ? ProfileActivity.class : MeActivity.class;
    }

}
