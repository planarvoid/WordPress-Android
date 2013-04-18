package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ActivitiesFragment;
import com.soundcloud.android.provider.Content;

import android.os.Bundle;

public class News extends ScActivity implements ScLandingPage{

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(mRootView.getContentHolderId(), ActivitiesFragment.create(Content.ME_ACTIVITIES)).commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_news;
    }
}
