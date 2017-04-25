package com.soundcloud.android.discovery;


import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;

import android.support.v4.app.Fragment;

public class DiscoveryNavigationTarget extends BaseNavigationTarget {

    public DiscoveryNavigationTarget() {
        super(R.string.tab_discovery, R.drawable.tab_discovery);
    }

    @Override
    public Fragment createFragment() {
        return new DiscoveryFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.DISCOVER;
    }

}
