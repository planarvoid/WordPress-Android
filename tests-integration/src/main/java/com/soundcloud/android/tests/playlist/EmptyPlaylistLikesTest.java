package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyPlaylistLikesTest extends ActivityTest<MainActivity> {

    protected PlaylistsScreen playlistsScreen;

    public EmptyPlaylistLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        emptyUser.logIn(getInstrumentation().getTargetContext());
        setDependsOn(Flag.NEW_LIKES_END_TO_END);

        super.setUp();

        playlistsScreen = NavigationHelper.openLikedPlaylists(new MenuScreen(solo));
    }

    public void testShowsEmptyLikesScreen() {
        assertTrue(playlistsScreen.emptyView().isVisible());
    }
}
