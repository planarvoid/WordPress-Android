package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class TrackCommentsScreen extends Screen {
    private static Class ACTIVITY = TrackCommentsActivity.class;

    public TrackCommentsScreen(Han solo) {
        super(solo);
    }

    public String getTitle() {
        return title().getText();
    }

    private ViewElement title() {
        return testDriver.findElement(With.id(R.id.playable_title));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
