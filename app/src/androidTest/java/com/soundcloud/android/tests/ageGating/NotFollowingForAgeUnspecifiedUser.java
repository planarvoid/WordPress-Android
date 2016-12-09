package com.soundcloud.android.tests.ageGating;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class NotFollowingForAgeUnspecifiedUser extends ActivityTest<LauncherActivity> {

    public NotFollowingForAgeUnspecifiedUser() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        // using one of the old users (as they didn't have to specify age on signup)
        return defaultUser;
    }

    // *** Disabling until Github Issue #2877 is fixed ***
    public void ignore_testUsersWithUnspecifiedAgeAreNotAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("annoymouse")
                .findAndClickFirstUserItem()
                .clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(false));
    }
}

