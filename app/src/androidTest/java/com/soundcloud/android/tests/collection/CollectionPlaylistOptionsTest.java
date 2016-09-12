package com.soundcloud.android.tests.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class CollectionPlaylistOptionsTest extends ActivityTest<MainActivity> {

    protected PlaylistsScreen playlistsScreen;

    public CollectionPlaylistOptionsTest() {
        super(MainActivity.class);
    }

    private void navigateToPlaylists() {
        playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();
    }

    @Override
    protected void logInHelper() {
        TestUser.collectionUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testFiltersPlaylists() {
        navigateToPlaylists();

        playlistsScreen.scrollToFirstPlaylist();

        int unfilteredCount = playlistsScreen.visiblePlaylistsCount();

        playlistsScreen.clickPlaylistOptions()
                       .clickCreated()
                       .clickDone();

        assertThat(playlistsScreen.visiblePlaylistsCount(), is(lessThan(unfilteredCount)));

        playlistsScreen.clickPlaylistOptions()
                       .clickLiked()
                       .clickDone();

        assertThat(playlistsScreen.visiblePlaylistsCount(), is(equalTo(unfilteredCount)));

        playlistsScreen.clickPlaylistOptions()
                       .clickCreated()
                       .clickDone();

        assertThat(playlistsScreen.visiblePlaylistsCount(), is(lessThan(unfilteredCount)));

        playlistsScreen.removeFilters();

        assertThat(playlistsScreen.visiblePlaylistsCount(), is(equalTo(unfilteredCount)));
    }

    public void testSortsPlaylists() {
        navigateToPlaylists();

        final String firstPlaylistTitle = playlistsScreen.scrollToFirstPlaylist().getTitle();

        playlistsScreen.clickPlaylistOptions()
                       .clickSortByTitle()
                       .clickDone();

        assertThat(playlistsScreen.scrollToFirstPlaylist().getTitle(), is(not(equalTo(firstPlaylistTitle))));

        playlistsScreen.clickPlaylistOptions()
                       .clickSortByCreatedAt()
                       .clickDone();

        assertThat(playlistsScreen.scrollToFirstPlaylist().getTitle(), is(equalTo(firstPlaylistTitle)));
    }

}
