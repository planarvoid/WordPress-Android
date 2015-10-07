package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.discovery.DiscoveryFragment;
import com.soundcloud.android.main.BaseNavigationTarget;

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
        return Screen.SEARCH_MAIN;
    }

}
