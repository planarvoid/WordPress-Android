package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.screens.elements.PlayerElement;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

public class LegacyPlayerScreen extends Screen implements PlayerElement {
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

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public String getTrackTitle() {
        return trackTitle().getText();
    }

    @Override
    public void swipeNext() {
        swipeLeft();
    }
}
