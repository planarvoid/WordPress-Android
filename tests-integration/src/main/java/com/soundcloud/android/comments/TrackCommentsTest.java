package com.soundcloud.android.comments;

import static com.soundcloud.android.tests.TestUser.testUser;
import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.helpers.NavigationHelper;

public class TrackCommentsTest extends ActivityTestCase<MainActivity> {
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

