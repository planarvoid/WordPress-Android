package com.soundcloud.android.tests.activities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.helpers.NavigationHelper;

public class ActivitiesTest extends ActivityTest<MainActivity> {

    private ActivitiesScreen activitiesScreen;

    public ActivitiesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.testUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        waiter = new Waiter(solo);
        menuScreen = new MenuScreen(solo);
        final StreamScreen streamScreen = new StreamScreen(solo);

        activitiesScreen = NavigationHelper.openActivities(streamScreen);
    }

    public void testNewFollowerGoesToProfile() {
        ProfileScreen profileScreen = activitiesScreen.clickFollower();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
    }

    // Until https://github.com/soundcloud/SoundCloud-Android/issues/2265 is fixed
    // we will not receive new "like activities" for a given user
    public void ignoreLikeGoesToProfile() {
        ProfileScreen profileScreen = activitiesScreen.clickLike();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
    }

    public void testRepostGoesToProfile() {
        ProfileScreen profileScreen = activitiesScreen.clickRepost();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
    }

    public void testCommentGoesToCommentsScreen() {
        TrackCommentsScreen commentScreen = activitiesScreen.clickComment();
        assertThat(commentScreen.getTitle(), not(isEmptyOrNullString()));
    }

}
