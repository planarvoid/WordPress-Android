package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.ListElement;

import android.widget.ListView;

public class TrackCommentsScreen extends Screen {
    private static Class ACTIVITY = TrackCommentsActivity.class;

    public TrackCommentsScreen(Han solo) {
        super(solo);
    }

    public String getTitle() {
        return new TextElement(title()).getText();
    }

    public ProfileScreen clickFirstUser() {
        getListView().getItemAt(0).click();
        return new ProfileScreen(testDriver);
    }

    private ListElement getListView() {
        return testDriver.findElement(With.className(ListView.class)).toListView();
    }

    private ViewElement title() {
        return testDriver.findElement(With.id(R.id.title));
    }

    public TrackCommentsScreen scrollToBottomOfComments() {
        getListView().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public int getCommentsCount() {
        return getListView().getItemCount();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
