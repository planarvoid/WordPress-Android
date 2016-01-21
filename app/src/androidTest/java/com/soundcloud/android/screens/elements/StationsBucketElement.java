package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
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
        wrapped.findOnScreenElement(With.text("View all")).click();
        return new ViewAllStationsScreen(testDriver);
    }

    public StationElement getFirstStation() {
        return new StationElement(testDriver, wrapped.findOnScreenElement(With.id(R.id.station_item)));
    }

    public StationElement findStation(String title) {
        return new StationElement(testDriver, testDriver.scrollToItem(
                With.id(R.id.station_item),
                StationElement.WithTitle(testDriver, title))
        );
    }

    public String getTitle() {
        return new TextElement(wrapped.findOnScreenElement(With.id(R.id.title))).getText();
    }

    public boolean isVisible() {
        return wrapped.isVisible();
    }

    public static With WithTitle(final Han testDriver, final String title) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return new StationsBucketElement(testDriver, view).getTitle().equals(title);
            }

            @Override
            public String getSelector() {
                return String.format("Stations bucket with title %s", title);
            }
        };
    }
}
