package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.by.With;

import android.widget.ToggleButton;
import android.view.View;

public class PlaylistDetailsScreen extends Screen {

    private static final Class ACTIVITY = PlaylistDetailActivity.class;
    private static final int TITLE = R.id.title;
    private static final int USERNAME = R.id.username;

    public PlaylistDetailsScreen(Han solo) {
        super(solo);
    }

    private View rootContainer() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.getView(android.R.id.list);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getUsername() {
        return title().getText();
    }

    public String getTitle() {
        return title().getText();
    }

    private ViewElement title() {
        return testDriver.findElement(With.id(TITLE));
    }

    private View username() {
        return rootContainer().findViewById(USERNAME);
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

}
