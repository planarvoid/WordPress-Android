package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ViewAllStationsScreen;

public class StationsBucketElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public StationsBucketElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public ViewAllStationsScreen clickViewAll() {
        wrapped.findElement(With.id(R.id.view_all)).click();
        return new ViewAllStationsScreen(testDriver);
    }

    public StationElement getFirstStation() {
        return new StationElement(testDriver, wrapped.findElement(With.id(R.id.station_item)));
    }

    public StationElement findStation(With matcher) {
        return new StationElement(testDriver, wrapped.findElement(matcher));
    }

    public boolean isVisible() {
        return wrapped.isVisible();
    }
}
