package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.provider.Content;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class PlaylistsPagerAdapter extends FragmentPagerAdapter {

    private static final int TAB_YOUR_PLAYLISTS = 0;
    private static final int TAB_LIKED_PLAYLISTS = 1;

    private final Resources resources;
    private final FeatureFlags featureFlags;

    public PlaylistsPagerAdapter(FragmentManager fragmentManager, Resources resources, FeatureFlags featureFlags) {
        super(fragmentManager);
        this.resources = resources;
        this.featureFlags = featureFlags;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case TAB_YOUR_PLAYLISTS:
                if(featureFlags.isEnabled(Flag.PLAYLIST_POSTS_FRAGMENT)) {
                    return new PlaylistPostsFragment();
                } else {
                    return ScListFragment.newInstance(Content.ME_PLAYLISTS.uri, R.string.side_menu_playlists, Screen.SIDE_MENU_PLAYLISTS, false);
                }
            case TAB_LIKED_PLAYLISTS:
                return new PlaylistLikesFragment();
            default:
                throw new IllegalArgumentException("Unexpected position for getItem " + position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_YOUR_PLAYLISTS:
                return resources.getString(R.string.your_playlists_tab);
            case TAB_LIKED_PLAYLISTS:
                return resources.getString(R.string.liked_playlists_tab);
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
