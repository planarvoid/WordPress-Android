package com.soundcloud.android.explore;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.explore.ExploreGenreScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

import static com.soundcloud.android.tests.TestUser.testUser;

public class ExploreGenres extends ActivityTestCase<MainActivity> {
    private ExploreScreen exploreScreen;
    private ExploreGenreScreen exploreGenreScreen;

    public ExploreGenres() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();
        exploreScreen = new ExploreScreen(this);
        exploreGenreScreen = new ExploreGenreScreen(this);
    }

    public void testElectronicMusicCategoryHasContent(){
        menuScreen.openExplore();
        assertEquals("GENRE is the default tab for explore", "GENRES", exploreScreen.currentTabTitle());
        exploreScreen.clickElectronicGenre();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryPullToRefresh(){
        menuScreen.openExplore();
        exploreScreen.clickElectronicGenre();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
        exploreGenreScreen.pullToRefresh();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryLoadsNextPageOfTracks(){
        menuScreen.openExplore();
        exploreScreen.clickElectronicGenre();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
        exploreGenreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional electronic music tracks on the list", 25, exploreGenreScreen.getItemsOnList());
    }
}
