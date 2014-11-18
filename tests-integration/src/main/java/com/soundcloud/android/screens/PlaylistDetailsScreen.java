package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class PlaylistDetailsScreen extends Screen {

    private static final Class ACTIVITY = PlaylistDetailActivity.class;
    private static final int TITLE = R.id.title;

    public PlaylistDetailsScreen(Han solo) {
        super(solo);
    }

    public void scrollToBottom() {
        tracksListElement().scrollToBottom();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getTitle() {
        return title().getText();
    }

    private ViewElement title() {
        return testDriver.findElement(With.id(TITLE));
    }

    private ViewElement headerPlayToggle() {
        return testDriver.findElement(With.id(R.id.toggle_play_pause));
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

    public VisualPlayerElement clickFirstTrack() {
        return clickNthTrack(0);
    }

    public VisualPlayerElement clickSecondTrack() {
        return clickNthTrack(1);
    }

    private VisualPlayerElement clickNthTrack(int trackIndex) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElement(With.id(android.R.id.list))
                .findElements(With.id(R.id.track_list_item))
                .get(trackIndex).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public PlaylistDetailsScreen scrollToFirstTrackItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.scrollListToLine(1);
        return this;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    private ListElement tracksListElement() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

}
