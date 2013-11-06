package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

public class PlayerScreen {
    protected Han solo;

    public PlayerScreen(Han solo) {
        this.solo = solo;
    }

    public String trackTitle() {
        TextView textView = (TextView)solo.getView(R.id.playable_title);
        return textView.getText().toString();
    }
}