package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class ProfilePagerAdapter extends FragmentPagerAdapter {

    public static final int FRAGMENT_COUNT = 4;

    protected static final int TAB_INFO = 0;
    protected static final int TAB_POSTS = 1;
    protected static final int TAB_PLAYLISTS = 2;
    protected static final int TAB_LIKES = 3;

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
        final Fragment item = (Fragment) super.instantiateItem(container, position);
        headerPresenter.registerScrollableFragment((ScrollableProfileItem) item);
        refreshHelper.addRefreshable(position, (RefreshAware) item);
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

            default:
                throw new IllegalArgumentException("Unexpected position for " + position);
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        headerPresenter.unregisterScrollableFragment((ScrollableProfileItem) object);
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
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public int getCount() {
        return FRAGMENT_COUNT;
    }

}
