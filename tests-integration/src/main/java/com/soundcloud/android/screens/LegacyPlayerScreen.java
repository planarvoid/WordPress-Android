package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.by.With;

public class LegacyPlayerScreen extends Screen {
    private static final Class ACTIVITY = PlayerActivity.class;

    public LegacyPlayerScreen(Han solo) {
        super(solo);
    }

    private ViewElement stopButton() {
        return testDriver.findElement(With.id(R.id.pause));
    }

    public ViewElement trackTitle() {
        return testDriver.findElement(With.id(R.id.playable_title));
    }

    public void stopPlayback() {
        stopButton().click();
    }

    public PlaylistDetailsScreen goBackToPlaylist() {
        testDriver.goBack();
        return new PlaylistDetailsScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getTrackTitle() {
        return trackTitle().getText();
    }
}
