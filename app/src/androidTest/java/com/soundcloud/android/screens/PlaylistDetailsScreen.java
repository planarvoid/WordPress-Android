package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
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
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public class PlaylistDetailsScreen extends Screen {

    private static final Class ACTIVITY = PlaylistDetailActivity.class;

    public PlaylistDetailsScreen(Han solo) {
        super(solo);
    }

    public PlaylistDetailsScreen scrollToBottom() {
        tracksListElement().scrollToBottom();
        return this;
    }

    public PlaylistOverflowMenu clickPlaylistOverflowButton() {
        testDriver
                .findElement(With.id(R.id.playlist_details_overflow_button))
                .click();
        return new PlaylistOverflowMenu(testDriver);
    }

    public DownloadImageViewElement headerDownloadElement() {
        return new DownloadImageViewElement(testDriver
                .findElement(With.id(R.id.header_download_state)));
    }

    public String getTitle() {
        return title().getText();
    }

    public boolean containsTrackWithTitle(String title) {
        return !(scrollToItem(
                With.id(R.id.track_list_item),
                TrackItemElement.WithTitle(testDriver, title)
        ) instanceof EmptyViewElement);
    }

    public VisualPlayerElement clickHeaderPlay() {
        headerPlayButton().click();
        waiter.waitForPlaybackToBePlaying();
        return new VisualPlayerElement(testDriver);
    }

    public void clickHeaderPause() {
        headerPlayButton().click();
        waiter.waitForPlaybackToBeIdle();
    }

    public boolean isPlayToggleChecked() {
        return headerPlayButton().isChecked();
    }

    public void touchToggleLike() {
        likeToggle().click();
    }

    public VisualPlayerElement clickFirstTrack() {
        return clickTrack(0);
    }

    public VisualPlayerElement clickLastTrack() {
        return scrollToLastTrackItem()
                .trackItemElements()
                .get(trackItemElements().size() - 1)
                .click();
    }

    public VisualPlayerElement startStationFromFirstTrack() {
        return findAndClickFirstTrackOverflowButton().clickStartStation();
    }

    public TrackItemElement scrollToAndGetFirstTrackItem() {
        return toTrackItemElement.apply(scrollToItem(With.id(R.id.track_list_item)));
    }

    public PlaylistDetailsScreen scrollToLastTrackItem() {
        testDriver.scrollListToLine(tracksListElement().getItemCount() - 1);
        return this;
    }

    public PlaylistDetailsScreen scrollToPosition(int position) {
        testDriver.scrollListToLine(position);
        return this;
    }

    public TrackItemMenuElement findAndClickFirstTrackOverflowButton() {
        return scrollToAndGetFirstTrackItem()
                .clickOverflowButton();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private TextElement title() {
        return new TextElement(testDriver.findElement(With.id(R.id.title)));
    }

    private ViewElement headerPlayButton() {
        return testDriver.findElement(With.id(R.id.btn_play));
    }

    private ViewElement likeToggle() {
        return testDriver.findElement(With.id(R.id.toggle_like));
    }

    public VisualPlayerElement clickTrack(int trackIndex) {
        return trackItemElements()
                .get(trackIndex)
                .click();
    }

    private ListElement tracksListElement() {
        return testDriver
                .findElement(With.id(android.R.id.list))
                .toListView();
    }

    public int getTrackCount() {
        return trackItemsList().getItemCount();
    }

    private List<TrackItemElement> trackItemElements() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(
                testDriver
                        .findElement(With.id(android.R.id.list))
                        .findElements(With.id(R.id.track_list_item)),
                toTrackItemElement
        );
    }

    private ListElement trackItemsList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = new Function<ViewElement, TrackItemElement>() {
        @Override
        public TrackItemElement apply(ViewElement viewElement) {
            return new TrackItemElement(testDriver, viewElement);
        }
    };
}
