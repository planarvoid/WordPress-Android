package com.soundcloud.android.tests.ageGating;

import static com.soundcloud.android.framework.TestUser.childUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class NotFollowingAsTooYoungUser extends ActivityTest<LauncherActivity> {

    public NotFollowingAsTooYoungUser() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        childUser.logIn(getInstrumentation().getTargetContext());
    }

    // *** Disabling until Github Issue #2877 is fixed ***/
    public void ignore_testBelow18UsersAreNotAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("annoymouse")
                .clickFirstUserItem()
                .clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(false));
    }
}

