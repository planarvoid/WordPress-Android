package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.playback.views.ArtworkTrackView;
import com.soundcloud.android.screens.elements.ViewElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.TextView;

public class LegacyPlayerScreen extends Screen {
    private static final Class ACTIVITY = PlayerActivity.class;

    public LegacyPlayerScreen(Han solo) {
        super(solo);
    }

    private ViewElement stopButton() {
        return solo.findElement(R.id.pause);
    }

    public ViewElement trackTitle() {
        return solo.findElement(R.id.playable_title);
    }

    public void stopPlayback() {
        stopButton().click();
    }

    public PlaylistDetailsScreen goBackToPlaylist() {
        solo.goBack();
        return new PlaylistDetailsScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getTrackTitle() {
        return trackTitle().getText();
    }
}
