package com.soundcloud.android.screens.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.StationElement;
import com.soundcloud.android.stations.RecentStationsActivity;

public class RecentStationsScreen extends Screen {
    private static final Class ACTIVITY = RecentStationsActivity.class;

    public RecentStationsScreen(Han testDriver) {
        super(testDriver);
    }

    public StationElement getFirstStation() {
        return new StationElement(testDriver, testDriver.findOnScreenElement(With.id(R.id.station_item)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}