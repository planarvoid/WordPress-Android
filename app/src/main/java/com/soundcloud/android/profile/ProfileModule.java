package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                LegacyProfileActivity.class,
                ProfileActivity.class,
                MeActivity.class,
                UserPostsFragment.class,
                UserPlaylistsFragment.class,
                UserLikesFragment.class,
                UserFollowingsFragment.class,
                UserFollowersFragment.class,
                UserDetailsFragment.class
        }, includes = AssociationsModule.class)
public class ProfileModule {

    @Provides
    ProfileApi provideProfileApi(Lazy<ProfileApiPublic> profileApi) {
        return profileApi.get();
    }

}
