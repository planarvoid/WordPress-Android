package com.soundcloud.android.screens.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.StationElement;
import com.soundcloud.android.stations.LikedStationsActivity;

public class LikedStationsScreen extends Screen {

    private static final Class ACTIVITY = LikedStationsActivity.class;

    public LikedStationsScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public StationHomeScreen clickStationWithTitle(String title) {
        stationWithTitle(title).click();
        return new StationHomeScreen(testDriver);
    }

    private ViewElement stationWithTitle(String title) {
        return scrollToItem(
                With.id(R.id.station_item),
                StationElement.WithTitle(testDriver, title)
        );
    }

}
