package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class PlaylistDetailsTest extends TrackingActivityTest<LauncherActivity> {
    private static final String TEST_PLAYLIST_SHUFFLE = "playlist-shuffle-events";

    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetailsTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistDetailsScreen = collectionScreen.clickPlaylistsPreview().clickOnFirstPlaylist();
    }

    public void testPlaylistDetailsScreenShouldBeVisibleOnPlaylistClick() {
        assertThat(playlistDetailsScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
    }

    public void testShufflePlaylist() {
        startEventTracking();

        VisualPlayerElement player = new VisualPlayerElement(solo);
        PlaylistOverflowMenu overflowMenu = playlistDetailsScreen.clickPlaylistOverflowButton();

        assertThat(player, is(not(visible())));
        overflowMenu.shuffle();
        assertThat(player, is(visible()));

        finishEventTracking(TEST_PLAYLIST_SHUFFLE);
    }

    public void testHeaderPlayClickShouldOpenPlayer() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        assertThat(player, is(not(visible())));
        final VisualPlayerElement visualPlayer = playlistDetailsScreen.clickHeaderPlay().waitForExpandedPlayer();
        assertThat(visualPlayer, is(expanded()));
    }

    public void disabled_testRemovingAndAddingTrackFromPlaylist() throws Exception {
        String title = playlistDetailsScreen.getTitle();
        int initialTrackCount = playlistDetailsScreen.getTrackCount();

        VisualPlayerElement player = playlistDetailsScreen.clickFirstTrack();
        assertTrue("Player did not expand", player.waitForExpandedPlayer().isExpanded());
        player.pressBackToCollapse();

        TrackItemMenuElement menu = playlistDetailsScreen.findAndClickFirstTrackOverflowButton();
        menu.clickRemoveFromPlaylist();

        assertThat(playlistDetailsScreen.getTrackCount(), is(initialTrackCount - 1));

        player.tapFooter();
        AddToPlaylistScreen addToPlaylistScreen = player.clickMenu().clickAddToPlaylist();
        addToPlaylistScreen.clickPlaylistWithTitleFromPlayer(title);
        player.pressBackToCollapse();

        assertThat(playlistDetailsScreen.getTrackCount(), is(initialTrackCount));
    }
}
