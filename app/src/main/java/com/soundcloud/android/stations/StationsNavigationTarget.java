package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;

import android.support.v4.app.Fragment;

public class StationsNavigationTarget extends BaseNavigationTarget {

    public StationsNavigationTarget() {
        super(R.string.tab_stations, R.drawable.tab_stations);
    }

    @Override
    public Fragment createFragment() {
        return new StationsHomeFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.STATIONS_HOME;
    }
}
