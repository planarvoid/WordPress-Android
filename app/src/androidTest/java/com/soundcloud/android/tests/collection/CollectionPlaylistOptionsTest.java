package com.soundcloud.android.tests.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.tests.ActivityTest;

@Ignore
public class CollectionPlaylistOptionsTest extends ActivityTest<MainActivity> {

    protected CollectionScreen collectionScreen;

    public CollectionPlaylistOptionsTest() {
        super(MainActivity.class);
    }

    private void navigateToCollections() {
        collectionScreen = mainNavHelper.goToCollections();
    }

    @Override
    protected void logInHelper() {
        TestUser.collectionUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testFiltersPlaylists() {
        navigateToCollections();

        int unfilteredCount = collectionScreen.getLoadedItemCount();

        collectionScreen.clickPlaylistOptions()
                .clickCreated()
                .clickDone();

        assertThat(collectionScreen.getLoadedItemCount(), is(lessThan(unfilteredCount)));

        collectionScreen.clickPlaylistOptions()
                .clickLiked()
                .clickDone();

        assertThat(collectionScreen.getLoadedItemCount(), is(equalTo(unfilteredCount)));

        collectionScreen.clickPlaylistOptions()
                .clickCreated()
                .clickDone();

        assertThat(collectionScreen.getLoadedItemCount(), is(lessThan(unfilteredCount)));

        collectionScreen.removeFilters();

        assertThat(collectionScreen.getLoadedItemCount(), is(equalTo(unfilteredCount)));
    }

    public void testSortsPlaylists() {
        navigateToCollections();

        final String firstPlaylistTitle = collectionScreen.scrollToFirstPlaylist().getTitle();

        collectionScreen.clickPlaylistOptions()
                .clickSortByTitle()
                .clickDone();

        assertThat(collectionScreen.scrollToFirstPlaylist().getTitle(), is(not(equalTo(firstPlaylistTitle))));

        collectionScreen.clickPlaylistOptions()
                .clickSortByCreatedAt()
                .clickDone();

        assertThat(collectionScreen.scrollToFirstPlaylist().getTitle(), is(equalTo(firstPlaylistTitle)));
    }
    
}
