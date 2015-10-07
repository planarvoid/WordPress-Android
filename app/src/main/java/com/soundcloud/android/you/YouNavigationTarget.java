package com.soundcloud.android.you;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.BaseNavigationTarget;

import android.support.v4.app.Fragment;

public class YouNavigationTarget extends BaseNavigationTarget {

    public YouNavigationTarget() {
        super(R.string.tab_you, R.drawable.tab_you);
    }

    @Override
    public Fragment createFragment() {
        // TODO: You screen does not exist yet
        return new Fragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.YOU;
    }

}
