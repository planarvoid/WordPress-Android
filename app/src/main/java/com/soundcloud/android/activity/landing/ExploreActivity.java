package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;

public class ExploreActivity extends ScActivity implements ScLandingPage
{
    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_explore;
    }
}
