package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

public class ExploreGenres extends ActivityTestCase<MainActivity> {
    private MainScreen mainScreen;
    private ExploreScreen exploreScreen;
    private ExploreGenreCategoryScreen categoryScreen;

    public ExploreGenres() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();
        mainScreen = new MainScreen(solo);
        exploreScreen = mainScreen.openExploreFromMenu();

    }

    public void testElectronicMusicCategoryHasContent(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals(15, categoryScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryPullToRefresh(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals(15, categoryScreen.getItemsOnList());

        exploreScreen.pullToRefresh();
        assertEquals(15, categoryScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryLoadsNextPageOfTracks(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals(15, categoryScreen.getItemsOnList());

        categoryScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertTrue(15 < categoryScreen.getItemsOnList());
    }
}
