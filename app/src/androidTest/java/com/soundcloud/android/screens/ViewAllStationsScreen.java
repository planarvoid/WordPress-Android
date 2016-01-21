package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.StationElement;
import com.soundcloud.android.stations.ShowAllStationsActivity;

public class ViewAllStationsScreen extends Screen {
    private static final Class ACTIVITY = ShowAllStationsActivity.class;

    public ViewAllStationsScreen(Han testDriver) {
        super(testDriver);
    }

    public StationElement getFirstStation() {
        return new StationElement(testDriver, testDriver.findOnScreenElement(With.id(R.id.station_item)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public StationElement findStation(With matcher) {
        return new StationElement(testDriver, testDriver.findOnScreenElement(matcher));
    }
}
