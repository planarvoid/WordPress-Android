package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import javax.annotation.Nullable;
import java.util.List;

public class TrackLikesScreen extends Screen {

    protected static final Class ACTIVITY = MainActivity.class;

    public TrackLikesScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement clickTrack(int index) {
        VisualPlayerElement visualPlayerElement = tracks()
                .get(index)
                .click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public VisualPlayerElement clickShuffleButton() {
        listHeaderShuffleButton().click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public int getLoadedTrackCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return tracks().size();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        likesList().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void waitForLikesDownloadToFinish() {
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

    public List<TrackItemElement> tracks(With with) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(testDriver.findElements(with), new Function<ViewElement, TrackItemElement>() {
            @Nullable
            @Override
            public TrackItemElement apply(@Nullable ViewElement viewElement) {
                return new TrackItemElement(testDriver, viewElement);
            }
        });
    }

    public List<TrackItemElement> tracks() {
        return tracks(With.id(R.id.track_list_item));
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        tracks()
                .get(0)
                .clickOverflowButton();
        return new TrackItemMenuElement(testDriver);
    }

    public DownloadImageViewElement headerDownloadElement() {
        return new DownloadImageViewElement(listHeader()
                .findElement(With.id(R.id.header_download_state)));
    }

    public SyncYourLikesScreen clickMakeAvailableOffline() {
        makeAvailableOfflineItem().click();
        return new SyncYourLikesScreen(testDriver);
    }

    public TrackLikesScreen clickListHeaderOverflowButton() {
        listHeaderOverflowButton().click();
        return this;
    }

    private ListElement likesList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

    private ViewElement makeAvailableOfflineItem() {
        return testDriver.findElement(text(testDriver.getString(R.string.make_offline_available)));
    }

    private ViewElement listHeaderShuffleButton() {
        return listHeader()
                .findElement(With.id(R.id.shuffle_btn));
    }

    private ViewElement listHeaderOverflowButton() {
        return listHeader()
                .findElement(With.id(R.id.overflow_button));
    }

    private ViewElement listHeader() {
        return testDriver
                .findElement(With.id(android.R.id.list))
                .findElement(With.id(R.id.header));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
