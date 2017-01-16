package com.soundcloud.android.profile;

import com.soundcloud.android.main.Screen;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

abstract class UserProfilePagerAdapter extends FragmentPagerAdapter {
    public UserProfilePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    abstract Screen getYourScreen(int position);

    abstract Screen getOtherScreen(int position);
}
