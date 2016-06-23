package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

class ProfilePagerAdapter extends FragmentPagerAdapter {

    public static final int FRAGMENT_COUNT = 4;

    protected static final int TAB_INFO = 0;
    protected static final int TAB_SOUNDS = 1;
    protected static final int TAB_FOLLOWINGS = 2;
    protected static final int TAB_FOLLOWERS = 3;

    private final Urn userUrn;
    private final Resources resources;
    private final ProfileScrollHelper activtyScrollHelper;
    private final boolean isLoggedInUser;
    private final SearchQuerySourceInfo searchQuerySourceInfo;

    ProfilePagerAdapter(FragmentActivity activity,
                        Urn userUrn,
                        boolean isLoggedInUser,
                        ProfileScrollHelper activtyScrollHelper,
                        SearchQuerySourceInfo searchQuerySourceInfo) {

        super(activity.getSupportFragmentManager());
        this.activtyScrollHelper = activtyScrollHelper;
        this.isLoggedInUser = isLoggedInUser;
        this.searchQuerySourceInfo = searchQuerySourceInfo;
        this.resources = activity.getResources();
        this.userUrn = userUrn;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final Fragment item = (Fragment) super.instantiateItem(container, position);
        if (item instanceof ProfileScreen) {
            activtyScrollHelper.addProfileCollection((ProfileScreen) item);
        }
        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof ProfileScreen) {
            activtyScrollHelper.removeProfileScreen((ProfileScreen) object);
        }
        super.destroyItem(container, position, object);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_INFO:
                return UserDetailsFragment.create(userUrn);
            case TAB_SOUNDS:
                return isLoggedInUser
                       ? UserSoundsFragment.createForCurrentUser(userUrn, Screen.USER_SOUNDS, searchQuerySourceInfo)
                       : UserSoundsFragment.create(userUrn, Screen.USER_SOUNDS, searchQuerySourceInfo);
            case TAB_FOLLOWINGS:
                return isLoggedInUser
                       ? MyFollowingsFragment.create(Screen.YOUR_FOLLOWINGS, searchQuerySourceInfo)
                       : UserFollowingsFragment.create(userUrn, Screen.USER_FOLLOWINGS, searchQuerySourceInfo);
            case TAB_FOLLOWERS:
                return isLoggedInUser
                       ?
                       UserFollowersFragment.createForCurrentUser(userUrn, Screen.USER_FOLLOWERS, searchQuerySourceInfo)
                       :
                       UserFollowersFragment.create(userUrn, Screen.USER_FOLLOWERS, searchQuerySourceInfo);
            default:
                throw new IllegalArgumentException("Unexpected position for " + position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_INFO:
                return resources.getString(R.string.tab_title_user_info);
            case TAB_SOUNDS:
                return resources.getString(R.string.tab_title_user_sounds);
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
