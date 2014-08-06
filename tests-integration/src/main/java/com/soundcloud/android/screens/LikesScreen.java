package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import java.util.List;

public class LikesScreen extends Screen {
    protected static final Class ACTIVITY = MainActivity.class;

    public LikesScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement clickItem(int index) {
        likesList().getItemAt(index).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public VisualPlayerElement clickLastTrack() {
        int size = tracks().size();
        tracks().get(size - 1).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    private ListElement likesList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

    private List<ViewElement> tracks() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.findElements(With.id(R.id.track_list_item));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
