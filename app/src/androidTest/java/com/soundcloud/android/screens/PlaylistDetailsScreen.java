package com.soundcloud.android.screens;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import java.util.List;

public class PlaylistDetailsScreen extends Screen {

    private static final Class ACTIVITY = PlaylistDetailActivity.class;
    private static final int TITLE = R.id.title;

    public PlaylistDetailsScreen(Han solo) {
        super(solo);
    }

    public PlaylistDetailsScreen scrollToBottom() {
        tracksListElement().scrollToBottom();
        return this;
    }

    public void clickBack() {
        testDriver.goBack();
    }

    public PlaylistOverflowMenu clickPlaylistOverflowButton() {
        testDriver
                .findElements(With.id(R.id.playlist_details_overflow_button))
                .get(0)
                .click();
        return new PlaylistOverflowMenu(testDriver);
    }

    public DownloadImageViewElement headerDownloadElement() {
        return new DownloadImageViewElement(testDriver
                .findElement(With.id(R.id.header_download_state)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getTitle() {
        return title().getText();
    }

    private TextElement title() {
        return new TextElement(testDriver.findElement(With.id(TITLE)));
    }

    private ViewElement headerPlayToggle() {
        return testDriver.findElement(With.id(R.id.toggle_play_pause));
    }

    /**
     * Use this method with caution, {@link Han} (TestDriver) might not
     * be able to find tracks in long or paginated lists.
     */
    public boolean containsTrackWithTitle(String title) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        tracksListElement().scrollToBottom();
        for (TrackItemElement trackItemElement: trackItemElements()) {
            if (trackItemElement.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }

    private ViewElement likeToggle() {
        return testDriver.findElement(With.id(R.id.toggle_like));
    }

    public void clickHeaderPlay() {
        headerPlayToggle().click();
        waiter.waitForPlaybackToBePlaying();
    }

    public void clickHeaderPause() {
        headerPlayToggle().click();
        waiter.waitForPlaybackToBeIdle();
    }

    public boolean isPlayToggleChecked() {
        return headerPlayToggle().isChecked();
    }

    public boolean isLiked() {
        return likeToggle().isChecked();
    }

    public void touchToggleLike() {
        likeToggle().click();
    }

    public VisualPlayerElement clickFirstTrack() {
        return clickNthTrack(0);
    }

    private VisualPlayerElement clickNthTrack(int trackIndex) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return trackItemElements()
                .get(trackIndex)
                .click();
    }

    public PlaylistDetailsScreen scrollToFirstTrackItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.scrollListToLine(1);
        return this;
    }

    public PlaylistDetailsScreen scrollToLastTrackItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.scrollListToLine(tracksListElement().getItemCount() - 1);
        return this;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return scrollToFirstTrackItem()
                .trackItemElements()
                .get(0)
                .clickOverflowButton();
    }

    public TrackItemMenuElement clickLastTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return scrollToLastTrackItem()
                .trackItemElements()
                .get(trackItemElements().size() - 1)
                .clickOverflowButton();
    }

    private ListElement tracksListElement() {
        return testDriver
                .findElement(With.id(android.R.id.list))
                .toListView();
    }

    private List<TrackItemElement> trackItemElements() {
        return Lists.transform(
                testDriver
                        .findElement(With.id(android.R.id.list))
                        .findElements(With.id(R.id.track_list_item)),
                toTrackItemElement
        );
    }

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = new Function<ViewElement, TrackItemElement>() {
        @Override
        public TrackItemElement apply(ViewElement viewElement) {
            return new TrackItemElement(testDriver, viewElement);
        }
    };
}
