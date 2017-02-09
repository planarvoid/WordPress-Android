package com.soundcloud.android.profile;


import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.FullscreenablePlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Named;

public class ProfileActivity extends FullscreenablePlayerActivity {

    public static final String EXTRA_USER_URN = "userUrn";
    public static final String EXTRA_SEARCH_QUERY_SOURCE_INFO = "searchQuerySourceInfo";

    @Inject @LightCycle ProfilePresenter profilePresenter;
    @Inject @Named(ProfileModule.SHOW_PROFILE_BANNER) boolean showProfileBanner;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject FeatureFlags featureFlags;

    public ProfileActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        // The activity is not a screen. Fragments are.
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        super.setActivityContentView();
        if (showProfileBanner) {
            baseLayoutHelper.createActionBarLayout(this, R.layout.profile);
        } else {
            baseLayoutHelper.createActionBarLayout(this, R.layout.profile_no_banner);
        }
    }

    @Override
    protected boolean shouldBeFullscreen() {
        return showProfileBanner;
    }


    static Urn getUserUrnFromIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_USER_URN)) {
            return intent.getParcelableExtra(EXTRA_USER_URN);
        } else if (intent.getData() != null) {
            return Urn.forUser(UriUtils.getLastSegmentAsLong(intent.getData()));
        } else {
            throw new IllegalStateException("User identifier not provided to Profile activity");
        }
    }
}
