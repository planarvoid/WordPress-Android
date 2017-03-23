package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class PlaylistDetailsTest extends TrackingActivityTest<LauncherActivity> {
    private static final String TEST_PLAYLIST_SHUFFLE = "playlist-shuffle-events";

    private PlaylistsScreen playlistsScreen;

    public PlaylistDetailsTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playlistUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistsScreen = collectionScreen.clickPlaylistsPreview();
    }

    public void testHeaderPlayClickShouldOpenPlayer() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        assertThat(player, is(not(visible())));
        final VisualPlayerElement visualPlayer = playlistsScreen.clickOnFirstPlaylist()
                                                                .clickHeaderPlay()
                                                                .waitForExpandedPlayer();
        assertThat(visualPlayer, is(expanded()));
    }

    public void testShufflePlaylist() {
        startEventTracking();

        VisualPlayerElement player = new VisualPlayerElement(solo);
        PlaylistOverflowMenu overflowMenu = playlistsScreen.clickOnFirstPlaylist()
                                                           .clickPlaylistOverflowButton();

        assertThat(player, is(not(visible())));
        overflowMenu.shuffle();
        assertThat(player, is(visible()));

        finishEventTracking(TEST_PLAYLIST_SHUFFLE);
    }

    // TODO remove this test when the new playlist screen is out
    // This feature changed and this behavior is actually covered in integration test.
    public void disabled_testRemovingAndAddingTrackFromPlaylist() throws Exception {
        setRequiredDisabledFeatures(Flag.NEW_PLAYLIST_SCREEN);
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.scrollToAndClickPlaylistWithTitle("whatever");
        int initialTrackCount = playlistDetailsScreen.getTrackCount();

        TrackItemMenuElement menu = playlistDetailsScreen.findAndClickFirstTrackOverflowButton();

        networkManagerClient.switchWifiOff(); // cheap way to not commit change to api
        menu.clickRemoveFromPlaylist();

        assertThat(playlistDetailsScreen.getTrackCount(), is(initialTrackCount - 1));
    }
}
