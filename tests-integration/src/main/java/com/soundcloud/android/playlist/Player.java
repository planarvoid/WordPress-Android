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

import java.lang.reflect.InvocationTargetException;

public class Player extends ActivityTestCase<MainActivity> {
    private VisualPlayerElement player;
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
        streamScreen.clickFirstTrack();
        assertThat(player(), is(Expanded()));
        player().pressBackToCollapse();

        openPlaylist();
        assertThat(player(), is(Visible()));
        assertThat(player(), is(Collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        openPlaylist();

        assertThat(player(), is(not(Visible())));
    }

    public void testPlayerAddTrackToPlaylist() {
        menuScreen.open()
                .clickLikes()
                .clickItem(1)
                .clickMenu()
                .addToPlaylistItem()
                .click();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        addToPlaylistsScreen.waitForDialog();
        assertThat(addToPlaylistsScreen, is(Visible()));
    }

    private VisualPlayerElement player() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (player == null) {
            player = new VisualPlayerElement(solo);
        }
        return player;
    }

    private void openPlaylist() {
        PlaylistScreen playlistScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistScreen.clickPlaylistAt(0);
    }

}
