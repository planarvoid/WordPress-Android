package com.soundcloud.android.profile;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module
public class ProfileModule {

    static final String PROFILE_SCROLL_HELPER = "profile_scroll_helper";

    @Provides
    ProfileApi provideProfileApi(ProfileApiMobile profileApi) {
        return profileApi;
    }

    @Named(PROFILE_SCROLL_HELPER)
    @Provides
    ProfileScrollHelper provideProfileScrollHelper(ProfileConfig profileConfig,
                                                   Lazy<ProfileScrollHelper> profileScrollHelperLazy,
                                                   Lazy<BannerProfileScrollHelper> bannerProfileScrollHelperLazy) {
        if (profileConfig.showProfileBanner()) {
            return bannerProfileScrollHelperLazy.get();
        } else {
            return profileScrollHelperLazy.get();
        }
    }
}
