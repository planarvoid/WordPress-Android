package com.soundcloud.android.profile;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class ProfilePagerAdapter extends FragmentPagerAdapter {

    protected static final int TAB_POSTS = 0;
    protected static final int TAB_LIKES = 1;
    public static final int FRAGMENT_COUNT = 6;

    private final ProfileHeaderPresenter headerPresenter;
    private final ProfilePagerRefreshHelper refreshHelper;

    public ProfilePagerAdapter(FragmentManager fm, ProfileHeaderPresenter headerPresenter,
                               ProfilePagerRefreshHelper refreshHelper) {
        super(fm);
        this.headerPresenter = headerPresenter;
        this.refreshHelper = refreshHelper;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final RefreshAware fragment = (RefreshAware) super.instantiateItem(container, position);
        refreshHelper.addRefreshable(position, fragment);
        headerPresenter.registerScrollableFragment((ScrollableProfileItem) fragment);
        return fragment;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_POSTS:
                return UserPostsFragment.create(Urn.forUser(67429938L), "Sc Empty", Screen.USER_POSTS, null);

            case TAB_LIKES:
                return UserPostsFragment.create(Urn.forUser(172720L), "Jon Schmidt", Screen.USER_POSTS, null);

            default:
                throw new IllegalArgumentException("Unexpected position for " + position);
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        refreshHelper.removeFragment(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_POSTS:
                return "user posts";
            case TAB_LIKES:
                return "user likes";
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}