package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.os.Bundle;

public class News extends ScActivity implements ScLandingPage{

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSupportActionBar().setTitle(getString(R.string.side_menu_activity));

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(mRootView.getContentHolderId(), ScListFragment.newInstance(Content.ME_ACTIVITIES)).commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_news;
    }
}
