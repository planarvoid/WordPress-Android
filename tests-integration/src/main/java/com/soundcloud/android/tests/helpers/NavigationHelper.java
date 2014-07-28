package com.soundcloud.android.tests.helpers;

import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class NavigationHelper {

    public static ExploreScreen openExploreFromMenu(ActivityTestCase activityTestCase) {
        return activityTestCase.getMenuScreen().open().clickExplore();
    }

    public static PlaylistTagsScreen openSearch(Screen screen) throws Exception {
        return screen.actionBar().clickSearchButton();
    }

    public static ActivitiesScreen openActivities(Screen screen) {
        return screen.actionBar().clickActivityOverflowButton();
    }
}
