package com.soundcloud.android.screens;

import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.with.With;

public class TrackInfoScreen extends Screen {

    public TrackInfoScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("info_dialog");
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public String getTitle() {
        return new TextElement(testDriver.findElement(With.id(com.soundcloud.android.R.id.title))).getText();
    }

    public ViewElement getNoDescription() {
        return testDriver.findElement(With.id(com.soundcloud.android.R.id.no_description));
    }

    public ViewElement getDescription() {
        return testDriver.findElement(With.id(com.soundcloud.android.R.id.description));
    }

    public TrackCommentsScreen clickComments() {
        testDriver.findElement(With.id(com.soundcloud.android.R.id.comments)).click();
        return new TrackCommentsScreen(testDriver);
    }

}
