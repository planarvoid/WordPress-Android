package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

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

    public VisualPlayerElement clickShuffleButton() {
        testDriver.findElement(With.text(testDriver.getString(R.string.shuffle))).click();
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

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
