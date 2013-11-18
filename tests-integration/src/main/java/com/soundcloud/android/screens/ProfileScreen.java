package com.soundcloud.android.screens;

import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.tests.Han;

public class ProfileScreen extends Screen{
    private Class ACTIVITY = ProfileActivity.class;
    protected Han solo;

    public ProfileScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String userName() {
        TextView username = (TextView) solo.getView(R.id.username);
        return username.getText().toString();
    }
}