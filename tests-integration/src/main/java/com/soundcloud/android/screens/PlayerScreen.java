package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

public class PlayerScreen extends Screen {
    private static final Class ACTIVITY = PlayerActivity.class;

    public PlayerScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(CONTENT_ROOT);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String trackTitle() {
        TextView textView = (TextView)solo.getView(R.id.playable_title);
        return textView.getText().toString();
    }
}
