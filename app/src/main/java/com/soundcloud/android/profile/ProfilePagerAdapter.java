package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

class ProfilePagerAdapter extends UserProfilePagerAdapter {
    static final int TAB_SOUNDS = 0;
    private static final int TAB_INFO = 1;
    private static final int FRAGMENT_COUNT = 2;

    private final Urn userUrn;
    private final Resources resources;
    private final boolean isLoggedInUser;
    private final SearchQuerySourceInfo searchQuerySourceInfo;

    ProfilePagerAdapter(FragmentActivity activity,
                        Urn userUrn,
                        boolean isLoggedInUser,
                        SearchQuerySourceInfo searchQuerySourceInfo) {

        super(activity.getSupportFragmentManager());
        this.isLoggedInUser = isLoggedInUser;
        this.searchQuerySourceInfo = searchQuerySourceInfo;
        this.resources = activity.getResources();
        this.userUrn = userUrn;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_SOUNDS:
                return isLoggedInUser
                       ? UserSoundsFragment.createForCurrentUser(userUrn, Screen.YOUR_MAIN, searchQuerySourceInfo)
                       : UserSoundsFragment.create(userUrn, Screen.USER_MAIN, searchQuerySourceInfo);
            case TAB_INFO:
                return UserDetailsFragment.create(userUrn, searchQuerySourceInfo);
            default:
                throw new IllegalArgumentException("Unexpected position for " + position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_SOUNDS:
                return resources.getString(R.string.tab_title_user_sounds);
            case TAB_INFO:
                return resources.getString(R.string.tab_title_user_info);
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public Screen getYourScreen(int position) {
        switch (position) {
            case TAB_SOUNDS:
                return Screen.YOUR_MAIN;
            case TAB_INFO:
                return Screen.YOUR_INFO;
            default:
                return Screen.UNKNOWN;
        }
    }

    @Override
    public Screen getOtherScreen(int position) {
        switch (position) {
            case TAB_SOUNDS:
                return Screen.USER_MAIN;
            case TAB_INFO:
                return Screen.USER_INFO;
            default:
                return Screen.UNKNOWN;
        }
    }

    @Override
    public int getCount() {
        return FRAGMENT_COUNT;
    }
}
