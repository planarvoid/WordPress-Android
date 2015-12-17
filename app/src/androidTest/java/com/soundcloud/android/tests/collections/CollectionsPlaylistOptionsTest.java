package com.soundcloud.android.tests.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.tests.ActivityTest;

@Ignore
public class CollectionsPlaylistOptionsTest extends ActivityTest<MainActivity> {

    protected CollectionsScreen collectionsScreen;

    public CollectionsPlaylistOptionsTest() {
        super(MainActivity.class);
    }

    private void navigateToCollections() {
        collectionsScreen = mainNavHelper.goToCollections();
    }

    @Override
    protected void logInHelper() {
        TestUser.collectionsUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testFiltersPlaylists() {
        navigateToCollections();

        int unfilteredCount = collectionsScreen.getLoadedItemCount();

        collectionsScreen.clickPlaylistOptions()
                .clickCreated()
                .clickDone();

        assertThat(collectionsScreen.getLoadedItemCount(), is(lessThan(unfilteredCount)));

        collectionsScreen.clickPlaylistOptions()
                .clickLiked()
                .clickDone();

        assertThat(collectionsScreen.getLoadedItemCount(), is(equalTo(unfilteredCount)));

        collectionsScreen.clickPlaylistOptions()
                .clickCreated()
                .clickDone();

        assertThat(collectionsScreen.getLoadedItemCount(), is(lessThan(unfilteredCount)));

        collectionsScreen.removeFilters();

        assertThat(collectionsScreen.getLoadedItemCount(), is(equalTo(unfilteredCount)));
    }

    public void testSortsPlaylists() {
        navigateToCollections();

        final String firstPlaylistTitle = collectionsScreen.scrollToFirstPlaylist().getTitle();

        collectionsScreen.clickPlaylistOptions()
                .clickSortByTitle()
                .clickDone();

        assertThat(collectionsScreen.scrollToFirstPlaylist().getTitle(), is(not(equalTo(firstPlaylistTitle))));

        collectionsScreen.clickPlaylistOptions()
                .clickSortByCreatedAt()
                .clickDone();

        assertThat(collectionsScreen.scrollToFirstPlaylist().getTitle(), is(equalTo(firstPlaylistTitle)));
    }
    
}
