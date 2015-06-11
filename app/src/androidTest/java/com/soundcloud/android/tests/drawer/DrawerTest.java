package com.soundcloud.android.tests.drawer;


import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

public class DrawerTest extends ActivityTest<LauncherActivity> {

    public DrawerTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testOpeningOverflowDoesNotCloseDrawer() {
        menuScreen.open();
        solo.openSystemMenu();
        solo.openSystemMenu();
        assertEquals("DrawerTest should be open when opening system menu", true, menuScreen.isOpened());
    }

    public void testDrawerClosesWhenOverflowMenuItemPicked() {
        menuScreen.open();
        solo.openSystemMenu();
        menuScreen.clickSystemSettings();
        assertEquals("DrawerTest should be closed after picking system menu item", false, menuScreen.isOpened());
    }

    public void testDrawerProfileButtonOpensProfile() {
        menuScreen.open();
        ProfileScreen profileScreen = menuScreen.clickUserProfile();
        assertEquals("Should go to user profile", "android-testing", profileScreen.getUserName());
    }

    public void testDrawerOpenExplore() {
        menuScreen.open();
        ExploreScreen exploreScreen = menuScreen.clickExplore();
        assertEquals("Should go to Explore screen", true, exploreScreen.isVisible());
    }

    public void testDrawerOpensLikes() {
        menuScreen.open();
        TrackLikesScreen likesScreen = menuScreen.clickLikes();
        assertEquals("Should go to Likes screen", true, likesScreen.isVisible());
    }

    public void testDrawerShowsPlaylists() {
        menuScreen.open();
        PlaylistsScreen playlistsScreen = menuScreen.clickPlaylists();
        assertEquals("Should go to Playlists screen", true, playlistsScreen.isVisible());
    }
}

