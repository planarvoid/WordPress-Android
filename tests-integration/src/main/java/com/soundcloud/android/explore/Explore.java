package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class Explore extends ActivityTestCase<MainActivity> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;
    private MenuScreen menuScreen;

    public Explore() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        menuScreen = new MenuScreen(solo);
        exploreScreen = openExploreFromMenu();
    }

    public void testTrendingMusicIsDisplayed() {
        exploreScreen.touchTrendingMusicTab();
        assertEquals("Current tab is MUSIC", "MUSIC", exploreScreen.currentTabTitle());
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTrendingMusicPullToRefresh() {
        exploreScreen.touchTrendingMusicTab();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());

        exploreScreen.pullToRefresh();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTendingMusicLoadsNextPage(){
        exploreScreen.touchTrendingMusicTab();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertTrue(15 < exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        exploreScreen.swipeLeft();

        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals("Current tab should be AUDIO", "AUDIO", exploreScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 15, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTrendingAudioIsDisplayedWhenTouchingTab() {
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals("Invalid number of tracks found", 15, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTrendingAudioPullToRefresh() {
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals("Invalid number of trending audio items", 15, exploreScreen.getItemsOnTrendingAudioList());

        exploreScreen.pullToRefresh();
        assertEquals("Invalid number of trending audio items", 15, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTendingAudioLoadsNextPage(){
        exploreScreen.touchTrendingAudioTab();
        int exploreTracksCountBefore = exploreScreen.getItemsOnTrendingAudioList();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertTrue( exploreTracksCountBefore < exploreScreen.getItemsOnTrendingAudioList());

    }

    public void testMusicIsDisplayedByDefault() {
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals("Music tab is displayed by default", "MUSIC", exploreScreen.currentTabTitle());
    }

    private ExploreScreen openExploreFromMenu() {
        return menuScreen.open()
                .clickExplore();
    }
}