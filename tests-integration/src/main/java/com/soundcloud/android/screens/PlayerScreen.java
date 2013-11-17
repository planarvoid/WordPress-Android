package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

public class PlayerScreen extends Screen {

    public PlayerScreen(Han solo) {
        super(solo);
    }

    public String trackTitle() {
        TextView textView = (TextView)solo.getView(R.id.playable_title);
        return textView.getText().toString();
    }
}
