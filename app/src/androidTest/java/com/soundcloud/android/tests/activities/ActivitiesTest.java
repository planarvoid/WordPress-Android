package com.soundcloud.android.tests.activities;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
    }

    public void testActivities() {
        ActivitiesScreen activitiesScreenFromMainNav = mainNavHelper.goToActivities();
        ActivitiesScreen activitiesScreenFromFollowersProfile = assertNewFollowerGoesToProfile(activitiesScreenFromMainNav);
        ActivitiesScreen activitiesScreenFromLikesProfile = assertLikeGoesToProfile(activitiesScreenFromFollowersProfile);
        ActivitiesScreen activitiesScreenFromRepostsProfile = assertRepostGoesToProfile(activitiesScreenFromLikesProfile);
        assertCommentGoesToCommentsScreen(activitiesScreenFromRepostsProfile);
    }

    private ActivitiesScreen assertNewFollowerGoesToProfile(ActivitiesScreen activitiesScreen) {
        ProfileScreen profileScreen = activitiesScreen.clickFollower();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
        return profileScreen.goBackToActivitiesScreen();
    }

    private ActivitiesScreen assertLikeGoesToProfile(ActivitiesScreen activitiesScreen) {
        ProfileScreen profileScreen = activitiesScreen.clickLike();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
        return profileScreen.goBackToActivitiesScreen();
    }

    private ActivitiesScreen assertRepostGoesToProfile(ActivitiesScreen activitiesScreen) {
        ProfileScreen profileScreen = activitiesScreen.clickRepost();
        assertThat(profileScreen.getUserName(), not(isEmptyOrNullString()));
        return profileScreen.goBackToActivitiesScreen();
    }

    private ActivitiesScreen assertCommentGoesToCommentsScreen(ActivitiesScreen activitiesScreen) {
        TrackCommentsScreen commentScreen = activitiesScreen.clickComment();
        assertThat(commentScreen.getTitle(), not(isEmptyOrNullString()));
        return assertClickingUserRowOpensUserProfile(commentScreen)
                .goBackToActivitiesScreen();
    }

    private static TrackCommentsScreen assertClickingUserRowOpensUserProfile(TrackCommentsScreen trackCommentsScreen) {
        ProfileScreen profileScreen = trackCommentsScreen.clickFirstUser();
        assertThat(profileScreen, is(visible()));
        return profileScreen.goBackToTrackCommentsScreen();
    }

}
