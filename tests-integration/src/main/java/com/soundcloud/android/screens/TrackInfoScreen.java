package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.viewelements.TextElement;
import com.soundcloud.android.tests.with.With;

public class TrackInfoScreen extends Screen {

    public TrackInfoScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public boolean waitForDialog() {
        return waiter.waitForFragmentByTag("info_dialog");
    }

    public String getTitle() {
        return new TextElement(testDriver.findElement(With.id(com.soundcloud.android.R.id.title))).getText();
    }

    public TrackCommentsScreen clickComments() {
        testDriver.findElement(With.id(com.soundcloud.android.R.id.comments)).click();
        return new TrackCommentsScreen(testDriver);
    }

}
