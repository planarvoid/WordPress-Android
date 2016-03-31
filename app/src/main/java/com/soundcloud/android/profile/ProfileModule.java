package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ProfileActivity.class,
                MyPostsFragment.class,
                MyPlaylistsFragment.class,
                MyLikesFragment.class,
                MyFollowingsFragment.class,
                UserPostsFragment.class,
                UserPlaylistsFragment.class,
                UserLikesFragment.class,
                UserFollowingsFragment.class,
                UserFollowersFragment.class,
                UserSoundsFragment.class,
                UserDetailsFragment.class,
                VerifyAgeActivity.class,
                UserRepostsActivity.class,
                UserRepostsFragment.class,
                UserTracksActivity.class,
                UserTracksFragment.class
        }, includes = AssociationsModule.class)
public class ProfileModule {

    @Provides
    ProfileApi provideProfileApi(Lazy<ProfileApiPublic> profileApiPublic,
                                 Lazy<ProfileApiMobile> profileApiPrivate) {
        return new ProfileApiDelegator(profileApiPublic, profileApiPrivate);
    }

}
