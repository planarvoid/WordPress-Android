package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

public class ProfileScreen {
    protected Han solo;

    public ProfileScreen(Han solo) {
        this.solo = solo;
    }

    public String userName() {
        TextView username = (TextView) solo.getView(R.id.username);
        return username.getText().toString();
    }
}