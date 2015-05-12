package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.StreamScreen;
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

    public void testAddTrackToPlaylistFromStream() {
        StreamScreen streamScreen = new StreamScreen(solo);
        String trackAddedTitle = streamScreen.firstTrack().getTitle();

        streamScreen
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist()
                .clickPlaylistWithTitleFromStream(TEST_PLAYLIST);

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    public void testAddTrackToPlaylistFromPlayer() {
        StreamScreen streamScreen = new StreamScreen(solo);
        String trackAddedTitle = streamScreen.firstTrack().getTitle();

        VisualPlayerElement player = streamScreen.clickFirstTrack();
        player.clickMenu()
                .clickAddToPlaylist()
                .clickPlaylistWithTitleFromPlayer(TEST_PLAYLIST)
                .pressBackToCollapse();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    private void assertPlaylistContainsTrack(String trackTitle) {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistAt(0);

        assertThat(playlistDetailsScreen.getTitle(), is(TEST_PLAYLIST));
        assertThat(playlistDetailsScreen.containsTrackWithTitle(trackTitle), is(true));

        playlistDetailsScreen.clickLastTrackOverflowButton().clickRemoveFromPlaylist();

        assertThat(playlistDetailsScreen.containsTrackWithTitle(trackTitle), is(false));
    }
}
