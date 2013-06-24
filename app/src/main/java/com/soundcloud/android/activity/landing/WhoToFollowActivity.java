package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class WhoToFollowActivity extends ScActivity implements ScLandingPage{
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_who_to_follow));
        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mRootView.getContentHolderId(), ScListFragment.newInstance(Content.SUGGESTED_USERS))
                    .commit();
        }
    }

    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_suggested_users;
    }
}