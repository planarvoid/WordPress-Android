package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.res.Resources;

import javax.inject.Inject;

class ProfileConfig {

    private final FeatureFlags featureFlags;
    private final Resources resources;

    @Inject
    ProfileConfig(FeatureFlags featureFlags,
                  Resources resources) {
        this.featureFlags = featureFlags;
        this.resources = resources;
    }

    boolean showProfileBanner() {
        // we only support M for colorizing the status bar and portrait as we haven't designed landscapr
        return featureFlags.isEnabled(Flag.PROFILE_BANNER)
                && resources.getBoolean(R.bool.profile_banner);
    }

    boolean hasAlignedUserInfo() {
        return featureFlags.isEnabled(Flag.ALIGNED_USER_INFO);
    }
}
