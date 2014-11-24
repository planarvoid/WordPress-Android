package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.screens.ActivitiesScreen;
import com.soundcloud.android.framework.screens.Screen;
import com.soundcloud.android.framework.screens.StreamScreen;
import com.soundcloud.android.framework.screens.WhoToFollowScreen;
import com.soundcloud.android.framework.screens.explore.ExploreScreen;
import com.soundcloud.android.framework.screens.search.PlaylistTagsScreen;

public class NavigationHelper {

    public static ExploreScreen openExploreFromMenu(StreamScreen screen) {
        return screen.openExploreFromMenu();
    }

    public static PlaylistTagsScreen openSearch(Screen screen) throws Exception {
        return screen.actionBar().clickSearchButton();
    }

    public static ActivitiesScreen openActivities(Screen screen) {
        return screen.actionBar().clickActivityOverflowButton();
    }

    public static WhoToFollowScreen openWhoToFollow(Screen screen) {
        return screen.actionBar().clickWhoToFollowOverflowButton();
    }
}
