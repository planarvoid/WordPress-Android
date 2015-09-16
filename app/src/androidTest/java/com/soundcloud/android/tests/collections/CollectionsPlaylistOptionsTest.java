package com.soundcloud.android.tests.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.ActivityTest;

public class CollectionsPlaylistOptionsTest extends ActivityTest<MainActivity> {

    protected CollectionsScreen collectionsScreen;

    public CollectionsPlaylistOptionsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.COLLECTIONS);
        super.setUp();

        menuScreen = new MenuScreen(solo);
        collectionsScreen = menuScreen.open().clickCollections();
    }

    @Override
    protected void logInHelper() {
        TestUser.collectionsUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testFiltersPlaylists() {

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

        menuScreen = new MenuScreen(solo);
        collectionsScreen = menuScreen.open().clickCollections();

        final String firstPlaylistTitle = collectionsScreen.getFirstPlaylistTitle();

        collectionsScreen.clickPlaylistOptions()
                .clickSortByTitle()
                .clickDone();

        assertThat(collectionsScreen.getFirstPlaylistTitle(), is(not(equalTo(firstPlaylistTitle))));

        collectionsScreen.clickPlaylistOptions()
                .clickSortByCreatedAt()
                .clickDone();

        assertThat(collectionsScreen.getFirstPlaylistTitle(), is(equalTo(firstPlaylistTitle)));
    }
}
