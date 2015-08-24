package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.StationsBucketElement;

public class StationsScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StationsScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stations_fragment");
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public StationsBucketElement getRecentStationsBucket() {
        return new StationsBucketElement(testDriver, testDriver.findElement(With.id(R.id.stations_bucket)));
    }
}
