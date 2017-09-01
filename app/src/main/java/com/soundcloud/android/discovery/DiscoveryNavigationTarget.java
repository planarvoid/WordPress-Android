package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.BaseNavigationTarget;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.optional.Optional;

import android.support.v4.app.Fragment;

public class DiscoveryNavigationTarget extends BaseNavigationTarget {

    private final FeatureFlags featureFlags;

    public DiscoveryNavigationTarget(FeatureFlags featureFlags) {
        super(R.string.tab_discovery, R.drawable.tab_discovery);
        this.featureFlags = featureFlags;
    }

    @Override
    public Fragment createFragment() {
        if (featureFlags.isEnabled(Flag.UNIFLOW_NEW_HOME)) {
            return new HomeFragment();
        } else {
            return new DiscoveryFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.DISCOVER;
    }

    @Override
    public Optional<Screen> getPageViewScreen() {
        return Optional.absent();
    }
}
