package com.soundcloud.android.framework.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.with.With;

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
