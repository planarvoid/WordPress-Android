package com.soundcloud.android.explore.genres;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class Explore extends ActivityTestCase<MainActivity> {
    private MainScreen mainScreen;
    private ExploreScreen exploreScreen;
    private ExploreGenreCategoryScreen categoryScreen;

    public Explore() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.testUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        mainScreen = new MainScreen(solo);
        exploreScreen = mainScreen.openExploreFromMenu();
    }

    public void testElectronicMusicCategoryHasContent(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertEquals(20, categoryScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryPullToRefresh(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertEquals(20, categoryScreen.getItemsOnList());

        exploreScreen.pullToRefresh();
        assertEquals(20, categoryScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryLoadsNextPageOfTracks(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertEquals(20, categoryScreen.getItemsOnList());

        categoryScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertTrue(20 < categoryScreen.getItemsOnList());
    }
}
