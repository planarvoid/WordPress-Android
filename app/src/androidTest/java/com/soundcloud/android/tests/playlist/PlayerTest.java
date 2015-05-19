package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

public class PlayerTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() throws Exception {
        final VisualPlayerElement playerElement = streamScreen.clickFirstTrack();
        assertThat(playerElement, is(expanded()));
        playerElement.pressBackToCollapse();

        openPlaylist();
        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        openPlaylist();

        assertThat(new VisualPlayerElement(solo), is(not(visible())));
    }

    public void testPlayerAddTrackToPlaylist() {
        final AddToPlaylistScreen addToPlaylistScreen = streamScreen.clickFirstTrack()
                .clickMenu()
                .clickAddToPlaylist();

        assertThat(addToPlaylistScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
    }

    private void openPlaylist() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylists();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistsScreen.clickPlaylistAt(0);
    }

}
