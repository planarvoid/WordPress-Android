package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.WhoToFollowScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;

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

    public static LikesScreen openLikedTracks(MenuScreen menu, Waiter waiter) {
        LikesScreen likesScreen = menu.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return likesScreen;
    }

    public static PlaylistsScreen openLikedPlaylists(MenuScreen screen) {
        PlaylistsScreen playlistsScreen = screen.open().clickPlaylist();
        playlistsScreen.touchLikedPlaylistsTab();
        return playlistsScreen;
    }

    public static PlaylistsScreen openPostedPlaylists(MenuScreen screen) {
        PlaylistsScreen playlistsScreen = screen.open().clickPlaylist();
        playlistsScreen.touchPostedPlaylistsTab();
        return playlistsScreen;
    }

}
