package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistItemsTest extends ActivityTest<MainActivity> {

    private static final String TEST_PLAYLIST = "MyPlaylist";

    public PlaylistItemsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.onePlaylistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testAddTrackToPlaylistFromStream() {
        StreamScreen streamScreen = new StreamScreen(solo);
        String trackAddedTitle = streamScreen.firstTrack().getTitle();

        streamScreen
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist()
                .clickPlaylistWithTitle(TEST_PLAYLIST);

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    public void testAddTrackToPlaylistFromPlayer() {
        StreamScreen streamScreen = new StreamScreen(solo);
        String trackAddedTitle = streamScreen.firstTrack().getTitle();

        VisualPlayerElement player = streamScreen.clickFirstTrack();
        assertTrue("Player did not expand", player.waitForExpandedPlayer());
        player.clickMenu()
              .clickAddToPlaylist()
              .clickPlaylistWithTitle(TEST_PLAYLIST);

        player.pressBackToCollapse();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    private void assertPlaylistContainsTrack(String trackTitle) {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistAt(0);

        String playListTitle = playlistDetailsScreen.getTitle();
        int trackCount = playlistsScreen.getLoadedTrackCount();

        assertThat(playListTitle, is(TEST_PLAYLIST));
        assertThat(playlistDetailsScreen.containsTrack(trackTitle), is(true));

        TrackItemMenuElement menu = playlistDetailsScreen.clickLastTrackOverflowButton();
        menu.clickRemoveFromPlaylist();

        assertThat(playlistsScreen.getLoadedTrackCount(), is(trackCount - 1));
    }
}
