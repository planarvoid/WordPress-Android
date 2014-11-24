package com.soundcloud.android.tests.drawer;


import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.tests.ActivityTest;

public class DrawerTest extends ActivityTest<LauncherActivity> {

    public DrawerTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
    }

    public void testOpeningOverflowDoesNotCloseDrawer() {
        menuScreen.open();
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
        LikesScreen likesScreen = menuScreen.clickLikes();
        assertEquals("Should go to Likes screen", true, likesScreen.isVisible());
    }

    public void testDrawerShowsPlaylists() {
        menuScreen.open();
        PlaylistScreen playlistScreen = menuScreen.clickPlaylist();
        assertEquals("Should go to Playlists screen", true, playlistScreen.isVisible());
    }
}

