package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
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
        waiter.waitForPlayerPlaying();
    }

    public void clickHeaderPause() {
        headerPlayToggle().click();
    }

    public boolean isPlayToggleChecked() {
        return headerPlayToggle().isChecked();
    }

    public VisualPlayerElement clickFirstTrack() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElement(With.id(android.R.id.list))
                .findElements(With.id(R.id.track_list_item))
                .get(0).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

}
