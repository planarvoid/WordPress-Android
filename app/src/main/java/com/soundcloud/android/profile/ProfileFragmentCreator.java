package com.soundcloud.android.profile;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.view.EmptyViewBuilder;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.net.Uri;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

/***
 * This class is just to serve as a bridge for ScListFragment and a new profile list fragments. It will go away with new profiles
 */
class ProfileFragmentCreator {

    @Inject
    ProfileFragmentCreator() {
        // for DI
    }

    Fragment create(Context context, String userName, Uri contentUri, Screen screen, SearchQuerySourceInfo searchQuerySource) {
        return createScListFragment(context, contentUri, screen, userName, searchQuerySource);
    }

    @NotNull
    private Fragment createScListFragment(Context context, Uri contentUri, Screen screen, String username, SearchQuerySourceInfo searchQuerySource) {
        ScListFragment listFragment = ScListFragment.newInstance(contentUri, username,screen, searchQuerySource);
        listFragment.setEmptyViewFactory(new EmptyViewBuilder().forContent(context, contentUri, username));
        return listFragment;
    }

}
