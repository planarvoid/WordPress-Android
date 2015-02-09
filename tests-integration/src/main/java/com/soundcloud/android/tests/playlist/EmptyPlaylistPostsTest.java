package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyPlaylistPostsTest extends ActivityTest<MainActivity> {

    protected PlaylistsScreen playlistsScreen;

    public EmptyPlaylistPostsTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        emptyUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testShowsEmptyLikesScreen() {
        playlistsScreen = NavigationHelper.openPostedPlaylists(new MenuScreen(solo));
        assertTrue(playlistsScreen.emptyView().isVisible());
    }
}
