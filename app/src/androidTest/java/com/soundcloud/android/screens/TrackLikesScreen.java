package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.likes.LikesActionBarElement;

import javax.annotation.Nullable;
import java.util.List;

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

    public void waitForLikesdownloadToFinish() {
        waiter.waitForTextToDisappear(testDriver.getString(R.string.offline_update_in_progress));
    }

    public boolean isDownloadInProgressTextVisible() {
        final String downloadInProgress = testDriver.getString(R.string.offline_update_in_progress);
        return testDriver.isElementDisplayed(text(downloadInProgress));
    }

    public boolean isLikedTracksTextVisible() {
        int count = tracks().size();
        final String downloadInProgress =
                testDriver.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, count, count);
        return testDriver.isElementDisplayed(text(downloadInProgress));
    }

    private ListElement likesList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

    public List<TrackItemElement> tracks(With with) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(testDriver.findElements(with), new Function<ViewElement, TrackItemElement>() {
            @Nullable
            @Override
            public TrackItemElement apply(@Nullable ViewElement viewElement) {
                return new TrackItemElement(viewElement);
            }
        });
    }

    public List<TrackItemElement> tracks() {
        return tracks(With.id(R.id.track_list_item));
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
