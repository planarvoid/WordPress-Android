package com.soundcloud.android.profile;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.EmptyViewBuilder;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.net.Uri;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

/**
 * This class is just to serve as a bridge for ScListFragment and a new profile list fragments. It will go away with new profiles
 */
class ProfileFragmentCreator {

    private final FeatureFlags featureFlags;

    @Inject
    ProfileFragmentCreator(FeatureFlags featureFlags) {
        // for DI
        this.featureFlags = featureFlags;
    }

    Fragment create(Context context, Content content, Urn userUrn, String userName, Uri contentUri, Screen screen, SearchQuerySourceInfo searchQuerySource) {
        if (featureFlags.isEnabled(Flag.NEW_PROFILE_FRAGMENTS)) {
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
                default:
                    return createScListFragment(context, contentUri, screen, userName, searchQuerySource);
            }

        } else {
            return createScListFragment(context, contentUri, screen, userName, searchQuerySource);
        }

    }

    @NotNull
    private Fragment createScListFragment(Context context, Uri contentUri, Screen screen, String username, SearchQuerySourceInfo searchQuerySource) {
        ScListFragment listFragment = ScListFragment.newInstance(contentUri, username, screen, searchQuerySource);
        listFragment.setEmptyViewFactory(new EmptyViewBuilder().forContent(context, contentUri, username));
        return listFragment;
    }

}
