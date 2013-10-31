package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.explore.ExploreGenreScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

public class ExploreGenres extends ActivityTestCase<Home> {
    private ExploreScreen exploreScreen;
    private ExploreGenreScreen exploreGenreScreen;

    public ExploreGenres() {
        super(Home.class);
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
        exploreScreen.touchGenresTab();
        exploreScreen.clickElectronicGenre();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryPullToRefresh(){
        menuScreen.openExplore();
        exploreScreen.touchGenresTab();
        exploreScreen.clickElectronicGenre();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
        exploreGenreScreen.pullToRefresh();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryLoadsNextPageOfTracks(){
        menuScreen.openExplore();
        exploreScreen.touchGenresTab();
        exploreScreen.clickElectronicGenre();
        assertEquals(15, exploreGenreScreen.getItemsOnList());
        exploreGenreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional electronic music tracks on the list", 25, exploreGenreScreen.getItemsOnList());
    }


    @Override
    protected void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }

}
