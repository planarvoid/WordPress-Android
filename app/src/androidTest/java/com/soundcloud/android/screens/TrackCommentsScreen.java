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
        waiter.waitForActivity(getActivity());
    }

    public String getTitle() {
        waiter.waitForActivity(getActivity());
        return new TextElement(title()).getText();
    }

    public ProfileScreen clickFirstUser() {
        getListView().getItemAt(0).click();
        return new ProfileScreen(testDriver);
    }

    public ActivitiesScreen goBackToActivitiesScreen() {
        testDriver.goBack();
        return new ActivitiesScreen(testDriver);
    }

    private ListElement getListView() {
        return testDriver.findOnScreenElement(With.className(ListView.class)).toListView();
    }

    private ViewElement title() {
        return testDriver.findOnScreenElement(With.id(R.id.title));
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
