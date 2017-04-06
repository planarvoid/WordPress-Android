package com.soundcloud.android.screens;


import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.FollowingsActivity;

public class FollowingsScreen extends Screen {

    public FollowingsScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return FollowingsActivity.class;
    }

    public ProfileScreen goBackToProfile() {
        testDriver.goBack();
        return new ProfileScreen(testDriver);
    }
}
