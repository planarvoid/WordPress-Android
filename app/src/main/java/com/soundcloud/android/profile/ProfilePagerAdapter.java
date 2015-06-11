package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

class ProfilePagerAdapter extends FragmentPagerAdapter {

    public static final int FRAGMENT_COUNT = 6;

    protected static final int TAB_INFO = 0;
    protected static final int TAB_POSTS = 1;
    protected static final int TAB_PLAYLISTS = 2;
    protected static final int TAB_LIKES = 3;
    protected static final int TAB_FOLLOWINGS = 4;
    protected static final int TAB_FOLLOWERS = 5;

    private final ProfileHeaderPresenter headerPresenter;
    private final ProfilePagerRefreshHelper refreshHelper;
    private final Urn userUrn;
    private final Resources resources;

    ProfilePagerAdapter(FragmentActivity activity,
                        ProfileHeaderPresenter headerPresenter,
                        ProfilePagerRefreshHelper refreshHelper,
                        Urn userUrn) {

        super(activity.getSupportFragmentManager());
        this.resources = activity.getResources();
        this.headerPresenter = headerPresenter;
        this.refreshHelper = refreshHelper;
        this.userUrn = userUrn;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final ProfileFragment item = (ProfileFragment) super.instantiateItem(container, position);
        headerPresenter.registerScrollableItem(item.getScrollableProfileItem());
        refreshHelper.addRefreshable(position, item.getRefreshableItem());
        return item;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_INFO:
                return UserDetailsFragment.create();

            case TAB_POSTS:
                return UserPostsFragment.create(userUrn, Screen.USER_POSTS, null /* TODO : SearchQueryInfo */);

            case TAB_PLAYLISTS:
                return UserPlaylistsFragment.create(userUrn, Screen.USER_PLAYLISTS, null /* TODO : SearchQueryInfo */);

            case TAB_LIKES:
                return UserLikesFragment.create(userUrn, Screen.USER_LIKES, null /* TODO : SearchQueryInfo */);

            case TAB_FOLLOWINGS:
                return UserFollowingsFragment.create(userUrn, Screen.USER_FOLLOWINGS, null /* TODO : SearchQueryInfo */);

            case TAB_FOLLOWERS:
                return UserFollowersFragment.create(userUrn, Screen.USER_FOLLOWERS, null /* TODO : SearchQueryInfo */);

            default:
                throw new IllegalArgumentException("Unexpected position for " + position);
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        final ProfileFragment profileFragment = (ProfileFragment) object;
        headerPresenter.unregisterScrollableFragment(profileFragment.getScrollableProfileItem());
        refreshHelper.removeFragment(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_INFO:
                return resources.getString(R.string.tab_title_user_info);
            case TAB_POSTS:
                return resources.getString(R.string.tab_title_user_posts);
            case TAB_PLAYLISTS:
                return resources.getString(R.string.tab_title_user_playlists);
            case TAB_LIKES:
                return resources.getString(R.string.tab_title_user_likes);
            case TAB_FOLLOWINGS:
                return resources.getString(R.string.tab_title_user_followings);
            case TAB_FOLLOWERS:
                return resources.getString(R.string.tab_title_user_followers);
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public int getCount() {
        return FRAGMENT_COUNT;
    }

}
