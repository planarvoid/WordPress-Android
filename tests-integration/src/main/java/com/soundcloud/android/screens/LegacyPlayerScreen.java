package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.TextView;

public class LegacyPlayerScreen extends Screen {
    private static final Class ACTIVITY = PlayerActivity.class;

    public LegacyPlayerScreen(Han solo) {
        super(solo);
    }

    public void stopPlayback() {
        solo.clickOnView(R.id.pause);
    }

    private View pauseButton() {
        waiter.waitForElement(R.id.pause);
        return solo.getView(R.id.pause);
    }

    public PlaylistDetailsScreen goBackToPlaylist() {
        solo.goBack();
        return new PlaylistDetailsScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public TextView trackTitleElement() {
        return (TextView)solo.getView(R.id.playable_title);
    }


    public String trackTitle() {
        TextView textView = (TextView)solo.getView(R.id.playable_title);
        return textView.getText().toString();
    }
}
