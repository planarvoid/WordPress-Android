package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.support.v4.app.Fragment;

public class StreamNavigationTarget extends BaseNavigationTarget {

    private final FeatureFlags featureFlags;

    public StreamNavigationTarget(FeatureFlags featureFlags, boolean isHome) {
        super(getTitle(isHome), getIcon(isHome));
        this.featureFlags = featureFlags;
    }

    @Override
    public Fragment createFragment() {
        if (featureFlags.isEnabled(Flag.UNIFLOW_STREAM)) {
            return new StreamUniflowFragment();
        } else {
            return new StreamFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.STREAM;
    }

    private static int getIcon(boolean isHome) {
        return isHome ? R.drawable.tab_home : R.drawable.tab_stream;
    }

    private static int getTitle(boolean isHome) {
        return isHome ? R.string.tab_home : R.string.tab_stream;
    }
}
