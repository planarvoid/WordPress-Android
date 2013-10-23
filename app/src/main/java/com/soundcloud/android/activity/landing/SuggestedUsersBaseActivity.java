package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;

import android.os.Bundle;

public abstract class SuggestedUsersBaseActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.frame_layout_holder);
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_suggested_users;
    }

}
