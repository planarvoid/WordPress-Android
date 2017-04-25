package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;

import android.support.v4.app.Fragment;

public class OldDiscoveryNavigationTarget extends BaseNavigationTarget {

    public OldDiscoveryNavigationTarget() {
        super(R.string.tab_discovery, R.drawable.tab_discovery);
    }

    @Override
    public Fragment createFragment() {
        return new OldDiscoveryFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.SEARCH_MAIN;
    }

}
