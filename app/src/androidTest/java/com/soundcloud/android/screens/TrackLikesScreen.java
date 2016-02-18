package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.LikesOverflowMenu;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.TrackLikesToolbarElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import javax.annotation.Nullable;
import java.util.List;

public class TrackLikesScreen extends Screen {

    protected static final Class ACTIVITY = MainActivity.class;

    private final TrackLikesToolbarElement toolbarElement;
    private final With updateInProgress = With.text(testDriver.getString(R.string.offline_update_in_progress));

    public TrackLikesScreen(Han solo) {
        super(solo);
        toolbarElement = new TrackLikesToolbarElement(solo);
    }

    public VisualPlayerElement clickTrack(int index) {
        VisualPlayerElement visualPlayerElement = tracks()
                .get(index)
                .click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public String getTrackTitle(int index) {
        return tracks().get(index).getTitle();
    }

    public VisualPlayerElement clickShuffleButton() {
        headerShuffleButton().click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public int getTotalLikesCount() {
        String text = headerText().getText();
        return Integer.parseInt(text.replaceAll("[^0-9]", ""));
    }

    public int getLoadedTrackCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return likesList().getItemCount();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        likesList().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public boolean waitForLikesDownloadToFinish() {
        waitForLikesToStartDownloading();
        return waiter.waitForNetworkCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !testDriver.isElementDisplayed(updateInProgress);
            }
        });
    }

    public void waitForLikesToStartDownloading() {
        waiter.waitForElement(headerText(), testDriver.getString(R.string.offline_update_in_progress));
    }

    public boolean isDownloadInProgressTextVisible() {
        final String downloadInProgress = testDriver.getString(R.string.offline_update_in_progress);
        return testDriver.isElementDisplayed(text(downloadInProgress));
    }

    public boolean isLikedTracksTextVisible() {
        int count = tracks().size();
        final String likedTracksCount =
                testDriver.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, count, count);
        return testDriver.isElementDisplayed(text(likedTracksCount));
    }

    public List<TrackItemElement> tracks(With with) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(testDriver.findOnScreenElements(with), new Function<ViewElement, TrackItemElement>() {
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

    public LikesOverflowMenu clickOverflowButton() {
        return toolbarElement.clickOverflowButton();
    }

    public ViewElement overflowButton() {
        return toolbarElement.overflowButton();
    }

    public DownloadImageViewElement headerDownloadElement() {
        return new DownloadImageViewElement(testDriver,
                header().findOnScreenElement(With.id(R.id.header_download_state)));
    }

    public CollectionScreen goBack() {
        testDriver.goBack();
        return new CollectionScreen(testDriver);
    }

    private RecyclerViewElement likesList() {
        return testDriver.findOnScreenElement(With.id(R.id.ak_recycler_view)).toRecyclerView();
    }

    private ViewElement headerShuffleButton() {
        return header()
                .findOnScreenElement(With.id(R.id.shuffle_btn));
    }

    private TextElement headerText() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.header_text)));
    }

    private ViewElement header() {
        return testDriver
                .findOnScreenElement(With.id(R.id.track_likes_header));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
