package com.soundcloud.android.tests.ageGating;

import static com.soundcloud.android.framework.TestUser.childUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class NotFollowingAsTooYoungUser extends ActivityTest<LauncherActivity> {

    private PlaylistTagsScreen playlistTagsScreen;

    public NotFollowingAsTooYoungUser() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        childUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        playlistTagsScreen = new StreamScreen(solo).actionBar().clickSearchButton();
    }

    // *** Disabling until Github Issue #2877 is fixed ***/
    public void ignore_testBelow18UsersAreNotAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = playlistTagsScreen
                .actionBar()
                .doSearch("annoymouse")
                .clickFirstUserItem()
                .clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(false));
    }

}

