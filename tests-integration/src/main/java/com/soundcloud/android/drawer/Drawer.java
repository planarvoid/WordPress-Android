package com.soundcloud.android.drawer;


import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;


public class Drawer extends ActivityTestCase<MainActivity> {

    public Drawer() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
    }

    public void testOpeningOverflowDoesNotCloseDrawer() throws Exception {
        menuScreen.clickHomeButton();
        solo.openSystemMenu();
        assertEquals("Drawer should be open when openning system menu", true, menuScreen.isOpened());
    }

    public void testDrawerClosesWhenOverflowMenuItemPicked() {
        menuScreen.clickHomeButton();
        solo.openSystemMenu();
        menuScreen.clickSystemSettings();
        assertEquals("Drawer should be closed after picking system menu item", false, menuScreen.isOpened());
    }

    public void testDrawerProfileButtonOpensProfile() {
        menuScreen.clickHomeButton();
        ProfileScreen profileScreen = menuScreen.clickProfile();
        assertEquals("Should go to user profile", "android-testing", profileScreen.userName());
    }

    public void testDrawerOpenExplore() {
        menuScreen.clickHomeButton();
        ExploreScreen exploreScreen = menuScreen.clickExplore();
        assertEquals("Should go to Explore screen", true, exploreScreen.isVisible());
    }

    public void testDrawerOpensLikes() {
        menuScreen.clickHomeButton();
        LikesScreen likesScreen = menuScreen.clickLikes();
        assertEquals("Should go to Likes screen", true, likesScreen.isVisible());
    }

    public void testDrawerShowsPlaylists() {
        menuScreen.clickHomeButton();
        PlaylistScreen playlistScreen = menuScreen.clickPlaylist();
        assertEquals("Should go to Likes screen", true, playlistScreen.isVisible());
    }
}

