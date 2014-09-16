package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.widget.ListView;

public class TrackCommentsScreen extends Screen {
    private static Class ACTIVITY = TrackCommentsActivity.class;

    public TrackCommentsScreen(Han solo) {
        super(solo);
    }

    public String getTitle() {
        return title().getText();
    }

    public ProfileScreen clickFirstUser() {
        getListView().getItemAt(0).click();
        return new ProfileScreen(testDriver);
    }

    private ListElement getListView() {
        return testDriver.findElement(With.className(ListView.class)).toListView();
    }

    private ViewElement title() {
        return testDriver.findElement(With.id(R.id.playable_title));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
