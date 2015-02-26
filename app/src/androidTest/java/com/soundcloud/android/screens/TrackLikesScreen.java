package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.likes.LikesActionBarElement;

import java.util.List;

import static com.soundcloud.android.framework.with.With.text;

public class TrackLikesScreen extends Screen {

    protected static final Class ACTIVITY = MainActivity.class;

    public TrackLikesScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement clickTrack(int index) {
        final int trackIndexInList = index + 1;
        likesList().getItemAt(trackIndexInList).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public VisualPlayerElement clickShuffleButton() {
        testDriver.findElement(text(testDriver.getString(R.string.shuffle))).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public int getLoadedTrackCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return likesList().getAdapter().getCount() - 1; // header
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        likesList().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void waitForLikesSyncToFinish() {
        waiter.waitForTextToDisappear(testDriver.getString(R.string.offline_update_in_progress));
    }

    public boolean isSyncInProgressTextVisible() {
        final String syncInProgress = testDriver.getString(R.string.offline_update_in_progress);
        return testDriver.isElementDisplayed(text(syncInProgress));
    }

    public boolean isLikedTracksTextVisible() {
        int count = tracks().size();
        final String syncInProgress =
                testDriver.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, count, count);
        return testDriver.isElementDisplayed(text(syncInProgress));
    }

    public boolean isSyncIconVisible() {
        return syncIcon().isVisible();
    }

    private ViewElement syncIcon() {
        return testDriver.findElement(With.id(R.id.sync_state));
    }

    private ListElement likesList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

    private List<ViewElement> tracks() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.findElements(With.id(R.id.track_list_item));
    }

    @Override
    public LikesActionBarElement actionBar() {
        return new LikesActionBarElement(testDriver);
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
