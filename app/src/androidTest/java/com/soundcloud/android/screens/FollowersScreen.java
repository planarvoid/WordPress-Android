package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.FollowersActivity;

public class FollowersScreen extends Screen {
    public FollowersScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return FollowersActivity.class;
    }

    public ProfileScreen goBackToProfile() {
        testDriver.goBack();
        return new ProfileScreen(testDriver);
    }
}
