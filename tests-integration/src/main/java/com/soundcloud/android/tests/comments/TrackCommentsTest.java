package com.soundcloud.android.tests.comments;

import static com.soundcloud.android.framework.TestUser.testUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.helpers.NavigationHelper;

public class TrackCommentsTest extends ActivityTest<MainActivity> {
    private TrackCommentsScreen trackCommentsScreen;

    public TrackCommentsTest() {
        super(MainActivity.class);
    }

    public void setUp() throws Exception {
        testUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        ActivitiesScreen activitiesScreen = NavigationHelper.openActivities(new StreamScreen(solo));
        trackCommentsScreen = activitiesScreen.clickComment();
    }

    public void testClickingUserRowOpensUserProfile() {
        ProfileScreen profileScreen = trackCommentsScreen.clickFirstUser();
        assertThat(profileScreen, is(visible()));
    }
}

