package com.soundcloud.android.profile;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

@Module
public class ProfileModule {

    @Provides
    ProfileApi provideProfileApi(Lazy<ProfileApiPublic> profileApiPublic,
                                 Lazy<ProfileApiMobile> profileApiPrivate) {
        return new ProfileApiDelegator(profileApiPublic, profileApiPrivate);
    }
}
