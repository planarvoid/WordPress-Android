package com.soundcloud.android.tests.activities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ActivitiesTest extends ActivityTest<MainActivity> {

    private ActivitiesScreen activitiesScreen;

    public ActivitiesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.testUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        waiter = new Waiter(solo);
        activitiesScreen = mainNavHelper.goToActivities();
    }

    public void testNewFollowerGoesToProfile() {
        ProfileScreen profileScreen = activitiesScreen.clickFollower();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
    }

    public void testLikeGoesToProfile() {
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
