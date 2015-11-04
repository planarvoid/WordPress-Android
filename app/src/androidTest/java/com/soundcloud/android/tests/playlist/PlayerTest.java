package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

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

    @BrokenScrollingTest
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
        CollectionsScreen playlistsScreen = mainNavHelper.goToCollections();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistsScreen.clickOnFirstPlaylist();
    }

}
