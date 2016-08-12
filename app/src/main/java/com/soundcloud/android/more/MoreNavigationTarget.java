package com.soundcloud.android.more;

import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;

import android.support.v4.app.Fragment;

public class MoreNavigationTarget extends BaseNavigationTarget {

    public MoreNavigationTarget() {
        super(R.string.tab_more, R.drawable.tab_more);
    }

    @Override
    public Fragment createFragment() {
        return new MoreFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.MORE;
    }

}
