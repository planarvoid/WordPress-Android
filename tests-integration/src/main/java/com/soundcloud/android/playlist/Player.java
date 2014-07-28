package com.soundcloud.android.playlist;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.Expanded;
import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

import java.lang.reflect.InvocationTargetException;

public class Player extends ActivityTestCase<MainActivity> {
    private VisualPlayerElement player;
    private StreamScreen streamScreen;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public Player() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.VISUAL_PLAYER);
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() throws Exception {
        streamScreen.clickFirstItem();
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
        playlistDetailsScreen = new PlaylistDetailsScreen(solo);
    }

}
