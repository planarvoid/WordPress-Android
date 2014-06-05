package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Han;

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
        return testDriver.findElement(TITLE);
    }

    private View username() {
        return rootContainer().findViewById(USERNAME);
    }

    public void clickHeaderPlay() {
        testDriver.clickOnView(R.id.toggle_play_pause);
        waiter.waitForPlayerPlaying();
    }

    public void clickHeaderPause() {
        testDriver.clickOnView(R.id.toggle_play_pause);
    }

    public boolean isPlayToggleChecked() {
        return ((ToggleButton) testDriver.getView(R.id.toggle_play_pause)).isChecked();
    }

}
