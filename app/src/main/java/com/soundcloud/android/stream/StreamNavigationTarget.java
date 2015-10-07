package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.BaseNavigationTarget;

import android.support.v4.app.Fragment;

public class StreamNavigationTarget extends BaseNavigationTarget {

    public StreamNavigationTarget() {
        super(R.string.tab_home, R.drawable.tab_home);
    }

    @Override
    public Fragment createFragment() {
        // TODO: We need a different way to set the onboarding state with this navigation model
        return SoundStreamFragment.create(false);
    }

    @Override
    public Screen getScreen() {
        return Screen.STREAM;
    }

}
