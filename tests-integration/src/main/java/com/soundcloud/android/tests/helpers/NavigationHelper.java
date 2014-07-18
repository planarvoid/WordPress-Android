package com.soundcloud.android.tests.helpers;

import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class NavigationHelper {

    public static ExploreScreen openExploreFromMenu(ActivityTestCase activityTestCase) {
        return activityTestCase.getMenuScreen().open().clickExplore();
    }

}
