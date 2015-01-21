package com.soundcloud.android.tests.playlist;

import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistLikesTest extends ActivityTest<MainActivity> {

    public PlaylistLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(),
                TestUser.playlistUser.getEmail(),
                TestUser.playlistUser.getPassword());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        setDependsOn(Flag.NEW_LIKES_END_TO_END);
        super.setUp();
    }

    public void testDrawerShowsPlaylists() {
        menuScreen.open();
        PlaylistsScreen playlistsScreen = menuScreen.clickPlaylist();
        playlistsScreen.touchLikedPlaylistsTab();
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistOnCurrentPageAt(0);
        assertEquals("Should go to Playlist screen", true, playlistDetailsScreen.isVisible());
    }

}
