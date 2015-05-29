package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyPlaylistLikesTest extends ActivityTest<MainActivity> {

    public EmptyPlaylistLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowsEmptyLikesScreen() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylists().touchLikedPlaylistsTab();
        assertTrue(playlistsScreen.emptyView().isVisible());
    }
}
