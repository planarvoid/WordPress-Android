package com.soundcloud.android.tests.ageGating;

import static com.soundcloud.android.framework.TestUser.over21user;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class FollowingAgeGatedUser extends ActivityTest<LauncherActivity> {

    private PlaylistTagsScreen playlistTagsScreen;

    public FollowingAgeGatedUser() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        over21user.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        playlistTagsScreen = new StreamScreen(solo).actionBar().clickSearchButton();
    }

    // *** Disable until we come up with a way to prevent syncing of certain events ***
    // This test is failing periodically because the unfollow action at the end of the test does not always get
    // synced, thus the next time this test is run, the user is still listed as being followed.
    public void ignore_testAbove21UsersAreAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = playlistTagsScreen
                .actionBar()
                .doSearch("annoymouse")
                .clickFirstUserItem()
                .clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(true));

        annoyMouseUserScreen.clickFollowToggle();
    }

}
