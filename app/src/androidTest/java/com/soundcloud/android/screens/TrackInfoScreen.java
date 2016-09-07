package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;

public class TrackInfoScreen extends Screen {

    public static final String FRAGMENT_TAG = "info_dialog";

    public TrackInfoScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public String getTitle() {
        return new TextElement(testDriver.findOnScreenElement(With.id(com.soundcloud.android.R.id.title))).getText();
    }

    public ViewElement getNoDescription() {
        return testDriver.findOnScreenElement(With.id(com.soundcloud.android.R.id.no_description));
    }

    public ViewElement getDescription() {
        return testDriver.findOnScreenElement(With.id(com.soundcloud.android.R.id.description));
    }

    public TrackCommentsScreen clickComments() {
        testDriver.findOnScreenElement(With.id(com.soundcloud.android.R.id.comments)).click();
        return new TrackCommentsScreen(testDriver);
    }

}