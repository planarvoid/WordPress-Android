package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BrokenScrollingTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.TrackItemElement;
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
        String trackAddedTitle = streamScreen.scrollToFirstTrack().trackTitle();

        streamScreen
                .clickFirstTrackCardOverflowButton()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(playlist)
                .clickDoneAndReturnToStream();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    @BrokenScrollingTest
    public void testAddTrackToPlaylistFromPlayer() {
        final TrackLikesScreen trackLikesScreen = mainNavHelper
                .goToCollections()
                .clickTrackLikes();
        final TrackItemElement track = trackLikesScreen
                .getTracks()
                .get(0);

        final String trackAddedTitle = track.getTitle();

        track.click().clickMenu()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(playlist)
                .clickDoneAndReturnToPlayer()
                .pressBackToCollapse();

        trackLikesScreen.goBack();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    private void assertPlaylistContainsTrack(String trackTitle) {
        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        collectionScreen.pullToRefresh();
        PlaylistDetailsScreen playlistDetailsScreen = collectionScreen.scrollToAndClickPlaylistWithTitle(playlist);

        assertThat(playlistDetailsScreen.getTitle(), is(playlist));
        assertThat(playlistDetailsScreen.containsTrackWithTitle(trackTitle), is(true));
    }
}
