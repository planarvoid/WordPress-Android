package com.soundcloud.android.explore;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

import static com.soundcloud.android.tests.TestUser.testUser;

public class Explore extends ActivityTestCase<MainActivity> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;

    public Explore() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        exploreScreen = new ExploreScreen(solo);
        waiter.waitForListContent();
    }

    public void testTrendingMusicIsDisplayed() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchTrendingMusicTab();
        assertEquals("Current tab is TRENDING MUSIC", "TRENDING MUSIC", exploreScreen.currentTabTitle());
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTrendingMusicPullToRefresh() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchTrendingMusicTab();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
        exploreScreen.pullToRefresh();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTendingMusicLoadsNextPage(){
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchTrendingMusicTab();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional trending tracks in list", 25, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.swipeLeft();
        exploreScreen.swipeLeft();
        assertEquals("Current tab should be TRENDING AUDIO", "TRENDING AUDIO", exploreScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 15, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTrendingAudioIsDisplayedWhenTouchingTab() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchTrendingAudioTab();
        assertEquals("Invalid number of genres found", 15, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTrendingAudioPullToRefresh() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchTrendingAudioTab();
        assertEquals("Invalid number of trending audio items", 15, exploreScreen.getItemsOnTrendingAudioList());
        exploreScreen.pullToRefresh();
        assertEquals("Invalid number of trending audio items", 15, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTendingAudioLoadsNextPage(){
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchTrendingAudioTab();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional trending tracks in list", 25, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testGenresAreDisplayedByDefault() {
        exploreScreen.openExploreFromMenu();
        assertEquals("Genres are displayed by default", "GENRES", exploreScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 22, exploreScreen.getNumberOfItemsInGenresTab());
    }
}