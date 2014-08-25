package com.soundcloud.android.playlist;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.Expanded;
import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class Player extends ActivityTestCase<MainActivity> {
    private StreamScreen streamScreen;

    public Player() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() throws Exception {
        final VisualPlayerElement playerElement = streamScreen.clickFirstTrack();
        assertThat(playerElement, is(Expanded()));
        playerElement.pressBackToCollapse();

        openPlaylist();
        assertThat(playerElement, is(Visible()));
        assertThat(playerElement, is(Collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        openPlaylist();

        assertThat(new VisualPlayerElement(solo), is(not(Visible())));
    }

    public void testPlayerAddTrackToPlaylist() {
        streamScreen.clickFirstTrack()
                .clickMenu()
                .addToPlaylistItem()
                .click();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        addToPlaylistsScreen.waitForDialog();
        assertThat(addToPlaylistsScreen, is(Visible()));
    }

    private void openPlaylist() {
        PlaylistScreen playlistScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistScreen.clickPlaylistAt(0);
    }

}
