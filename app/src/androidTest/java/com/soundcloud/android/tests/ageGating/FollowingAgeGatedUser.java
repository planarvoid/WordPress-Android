package com.soundcloud.android.tests.ageGating;

import static com.soundcloud.android.framework.TestUser.over21user;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class FollowingAgeGatedUser extends ActivityTest<LauncherActivity> {

    public FollowingAgeGatedUser() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return over21user;
    }

    // *** Disable until we come up with a way to prevent syncing of certain events ***
    // This test is failing periodically because the unfollow action at the end of the test does not always get
    // synced, thus the next time this test is run, the user is still listed as being followed.
    public void ignore_testAbove21UsersAreAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("annoymouse")
                .findAndClickFirstUserItem()
                .clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(true));

        annoyMouseUserScreen.clickFollowToggle();
    }
}
