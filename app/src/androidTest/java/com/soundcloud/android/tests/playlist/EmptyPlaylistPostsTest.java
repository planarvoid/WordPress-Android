package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyPlaylistPostsTest extends ActivityTest<MainActivity> {

    public EmptyPlaylistPostsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowsEmptyPostsScreen() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylists().touchPostedPlaylistsTab();
        assertTrue(playlistsScreen.emptyView().isVisible());
    }
}
