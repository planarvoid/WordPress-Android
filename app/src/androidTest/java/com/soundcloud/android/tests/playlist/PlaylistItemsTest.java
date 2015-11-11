package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BrokenScrollingTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistItemsTest extends ActivityTest<MainActivity> {

    private String playlist;

    public PlaylistItemsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.addToPlaylistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        playlist = "Playlist " + System.currentTimeMillis();
    }

    @BrokenScrollingTest
    public void testAddTrackToPlaylistFromStream() {
        StreamScreen streamScreen = new StreamScreen(solo);
        String trackAddedTitle = streamScreen.getTrackCard(0).trackTitle();

        streamScreen
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(playlist)
                .clickDoneAndReturnToStream();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    @BrokenScrollingTest
    public void testAddTrackToPlaylistFromPlayer() {
        StreamScreen streamScreen = new StreamScreen(solo);
        String trackAddedTitle = streamScreen.getTrackCard(0).trackTitle();

        VisualPlayerElement player = streamScreen.clickTrack(0);
        player.clickMenu()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(playlist)
                .clickDoneAndReturnToPlayer()
                .pressBackToCollapse();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    private void assertPlaylistContainsTrack(String trackTitle) {
        PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .clickPlaylistWithTitle(playlist);

        assertThat(playlistDetailsScreen.getTitle(), is(playlist));
        assertThat(playlistDetailsScreen.containsTrackWithTitle(trackTitle), is(true));
    }
}
