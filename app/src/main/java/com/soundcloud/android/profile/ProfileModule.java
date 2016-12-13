package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Named;

@Module
public class ProfileModule {

    static final String SHOW_PROFILE_BANNER = "show_profile_banner";
    static final String PROFILE_SCROLL_HELPER = "profile_scroll_helper";

    @Provides
    ProfileApi provideProfileApi(Lazy<ProfileApiPublic> profileApiPublic,
                                 Lazy<ProfileApiMobile> profileApiPrivate) {
        return new ProfileApiDelegator(profileApiPublic, profileApiPrivate);
    }

    @Named(SHOW_PROFILE_BANNER)
    @Provides
    boolean showProfileBanner(FeatureFlags featureFlags, Resources resources) {
        // we only support M for colorizing the status bar and portrait as we haven't designed landscapr
        return featureFlags.isEnabled(Flag.PROFILE_BANNER)
                && resources.getBoolean(R.bool.profile_banner);
    }

    @Named(PROFILE_SCROLL_HELPER)
    @Provides
    ProfileScrollHelper provideProfileScrollHelper(@Named(SHOW_PROFILE_BANNER) boolean showProfileBanner,
                                                   Lazy<ProfileScrollHelper> profileScrollHelperLazy,
                                                   Lazy<BannerProfileScrollHelper> bannerProfileScrollHelperLazy) {
        if (showProfileBanner) {
            return bannerProfileScrollHelperLazy.get();
        } else {
            return profileScrollHelperLazy.get();
        }
    }
}
