package com.soundcloud.android.profile;

import com.soundcloud.android.R;

import android.content.res.Resources;

import javax.inject.Inject;

class ProfileConfig {

    private final Resources resources;

    @Inject
    ProfileConfig(Resources resources) {
        this.resources = resources;
    }

    boolean showProfileBanner() {
        // we only support M for colorizing the status bar and portrait as we haven't designed landscape
        return resources.getBoolean(R.bool.profile_banner);
    }

}
