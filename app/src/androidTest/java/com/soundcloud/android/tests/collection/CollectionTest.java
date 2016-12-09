package com.soundcloud.android.tests.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.Matchers;

public class CollectionTest extends ActivityTest<MainActivity> {

    public CollectionTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.collectionUser;
    }

    public void testCollection() {
        CollectionScreen collectionScreenFromMainNav = mainNavHelper.goToCollections();
        CollectionScreen collectionScreenFromLikesPage = assertGoingToTrackLikesScreen(collectionScreenFromMainNav);
        CollectionScreen collectionScreenFromPlaylistsPage = assertGoingToPlaylistsPage(collectionScreenFromLikesPage);

        PlaylistsScreen playlistsScreen = collectionScreenFromPlaylistsPage.clickPlaylistsPreview();

        assertPlaylistFiltering(playlistsScreen);
        assertPlaylistSorting(playlistsScreen);
    }

    private CollectionScreen assertGoingToTrackLikesScreen(CollectionScreen collectionScreen) {
        TrackLikesScreen trackLikesScreen = collectionScreen.clickLikedTracksPreview();
        assertThat(trackLikesScreen.isVisible(), is(true));
        return trackLikesScreen.goBack();
    }

    private CollectionScreen assertGoingToPlaylistsPage(CollectionScreen collectionScreen) {
        PlaylistsScreen playlistsScreen = collectionScreen.clickPlaylistsPreview();
        assertThat(playlistsScreen.isVisible(), is(true));
        return playlistsScreen.goBackToCollections();
    }

    private void assertPlaylistFiltering(PlaylistsScreen playlistsScreen) {
        playlistsScreen.scrollToFirstPlaylist();

        int unfilteredCount = playlistsScreen.visiblePlaylistsCount();

        playlistsScreen.clickPlaylistOptions()
                       .clickCreated()
                       .clickDone();

        assertThat(playlistsScreen.visiblePlaylistsCount(), Matchers.is(lessThan(unfilteredCount)));

        playlistsScreen.clickPlaylistOptions()
                       .clickLiked()
                       .clickDone();

        assertThat(playlistsScreen.visiblePlaylistsCount(), Matchers.is(equalTo(unfilteredCount)));

        playlistsScreen.clickPlaylistOptions()
                       .clickCreated()
                       .clickDone();

        assertThat(playlistsScreen.visiblePlaylistsCount(), Matchers.is(lessThan(unfilteredCount)));

        playlistsScreen.removeFilters();

        assertThat(playlistsScreen.visiblePlaylistsCount(), Matchers.is(equalTo(unfilteredCount)));
    }

    private void assertPlaylistSorting(PlaylistsScreen playlistsScreen) {
        final String firstPlaylistTitle = playlistsScreen.scrollToFirstPlaylist().getTitle();

        playlistsScreen.clickPlaylistOptions()
                       .clickSortByTitle()
                       .clickDone();

        assertThat(playlistsScreen.scrollToFirstPlaylist().getTitle(), Matchers.is(not(equalTo(firstPlaylistTitle))));

        playlistsScreen.clickPlaylistOptions()
                       .clickSortByCreatedAt()
                       .clickDone();

        assertThat(playlistsScreen.scrollToFirstPlaylist().getTitle(), Matchers.is(equalTo(firstPlaylistTitle)));
    }
}
