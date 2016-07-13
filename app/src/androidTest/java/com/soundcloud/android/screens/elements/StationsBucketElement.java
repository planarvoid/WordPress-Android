package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class StationsBucketElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public StationsBucketElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public StationElement getFirstStation() {
        return new StationElement(testDriver, testDriver.scrollToItem(With.id(R.id.station_item)));
    }

    public String getTitle() {
        return new TextElement(wrapped.findOnScreenElement(With.id(R.id.title))).getText();
    }

    public boolean isVisible() {
        return wrapped.isOnScreen();
    }

}
