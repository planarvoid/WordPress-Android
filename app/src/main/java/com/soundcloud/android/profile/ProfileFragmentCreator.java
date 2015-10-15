package com.soundcloud.android.profile;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;

import android.support.v4.app.Fragment;

import javax.inject.Inject;

/**
 * This class is just to serve as a bridge for ScListFragment and a new profile list fragments. It will go away with new profiles
 */
class ProfileFragmentCreator {

    @Inject
    ProfileFragmentCreator() {
        // for DI
    }

    Fragment create(Content content, Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySource) {
        switch (content) {
            case ME_SOUNDS:
                return MyPostsFragment.create(screen, searchQuerySource);
            case ME_PLAYLISTS:
                return MyPlaylistsFragment.create(screen, searchQuerySource);
            case ME_LIKES:
                return MyLikesFragment.create(screen, searchQuerySource);
            case ME_FOLLOWINGS:
                return MyFollowingsFragment.create(screen, searchQuerySource);
            case USER_SOUNDS:
                return UserPostsFragment.create(userUrn, screen, searchQuerySource);
            case USER_PLAYLISTS:
                return UserPlaylistsFragment.create(userUrn, screen, searchQuerySource);
            case USER_LIKES:
                return UserLikesFragment.create(userUrn, screen, searchQuerySource);
            case USER_FOLLOWINGS:
                return UserFollowingsFragment.create(userUrn, screen, searchQuerySource);
            case USER_FOLLOWERS:
                return UserFollowersFragment.create(userUrn, screen, searchQuerySource);
            case ME_FOLLOWERS:
                return UserFollowersFragment.createForCurrentUser(userUrn, screen, searchQuerySource);
            default:
                throw new IllegalArgumentException("Content type not recognized " + content);
        }
    }
}
