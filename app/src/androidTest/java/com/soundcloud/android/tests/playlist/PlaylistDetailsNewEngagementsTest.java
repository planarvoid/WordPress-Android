package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistDetailsNewEngagementsTest extends ActivityTest<LauncherActivity> {

    private PlaylistsScreen playlistsScreen;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetailsNewEngagementsTest() {
        super(LauncherActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Flag.NEW_PLAYLIST_ENGAGEMENTS);
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistDetailsScreen = playlistsScreen.clickPlaylistAt(0);
    }

    public void testShufflePlaylistShowsPlayer() throws Exception {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        PlaylistOverflowMenu overflowMenu = playlistDetailsScreen.clickPlaylistOverflowButton();

        assertThat(player, is(not(visible())));
        overflowMenu.shuffle();
        assertThat(player, is(visible()));
    }
}
