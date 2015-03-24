package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyPlaylistLikesTest extends ActivityTest<MainActivity> {

    protected PlaylistsScreen playlistsScreen;

    public EmptyPlaylistLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowsEmptyLikesScreen() {
        playlistsScreen = NavigationHelper.openLikedPlaylists(new MenuScreen(solo));
        assertTrue(playlistsScreen.emptyView().isVisible());
    }
}
