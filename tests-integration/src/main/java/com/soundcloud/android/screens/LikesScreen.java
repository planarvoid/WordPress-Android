package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

public class LikesScreen extends Screen {
    protected static final Class ACTIVITY = MainActivity.class;

    public LikesScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement clickItem(int index) {
        likesList().getItemAt(index).click();
        waiter.waitForExpandedPlayer();
        return new VisualPlayerElement(testDriver);
    }

    private ListElement likesList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }
    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
