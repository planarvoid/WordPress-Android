package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;

import android.support.v4.app.Fragment;

public class StreamNavigationTarget extends BaseNavigationTarget {

    public StreamNavigationTarget(boolean isHome) {
        super(R.string.tab_home, isHome ? R.drawable.tab_home : R.drawable.tab_stream);
    }

    @Override
    public Fragment createFragment() {
        return new StreamFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.STREAM;
    }

}
