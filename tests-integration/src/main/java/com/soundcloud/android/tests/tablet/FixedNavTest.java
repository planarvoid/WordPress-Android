package com.soundcloud.android.tests.tablet;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreenTablet;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TabletTest;

@TabletTest
public class FixedNavTest extends ActivityTest<MainActivity> {

    private MenuScreenTablet tabletMenu;

    public FixedNavTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
        tabletMenu = new MenuScreenTablet(solo);
    }

    public void ignoreFixedNavProfileButtonOpensProfile() {
        ProfileScreen profileScreen = tabletMenu.clickUserProfile();
        assertEquals("Should go to user profile", "android-testing", profileScreen.getUserName());
        assertEquals("Nav should not be visible on tablet PROFILE", false, tabletMenu.isOpened());
    }

    public void ignoreFixedNavOpenExplore() {
        ExploreScreen exploreScreen = tabletMenu.clickExplore();
        assertEquals("Should go to Explore screen", true, exploreScreen.isVisible());
        assertEquals("Nav should be visible on tablet EXPLORE", true, tabletMenu.isOpened());
    }

    public void ignoreFixedNavOpensLikes() {
        LikesScreen likesScreen = tabletMenu.clickLikes();
        assertEquals("Should go to Likes screen", true, likesScreen.isVisible());
        assertEquals("Nav should be visible on tablet LIKES", true, tabletMenu.isOpened());
    }

    public void ignoreFixedNavShowsPlaylists() {
        PlaylistScreen playlistScreen = tabletMenu.clickPlaylist();
        assertEquals("Should go to Likes screen", true, playlistScreen.isVisible());
        assertEquals("Nav should be visible on tablet PLAYLISTS", true, tabletMenu.isOpened());
    }

    public void ignoreFixedNavIsVisibleOnWhenAppOpens() {
        assertTrue(tabletMenu.isOpened());
    }
}
