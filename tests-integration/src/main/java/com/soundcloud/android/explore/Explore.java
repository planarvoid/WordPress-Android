package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.MainActivity;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

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
        waiter.waitForListContent();
        exploreScreen = new ExploreScreen(this);
    }

    public void testTrendingMusicIsDisplayed() {
        exploreScreen.openExploreFromMenu();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTrendingMusicPullToRefresh() {
        exploreScreen.openExploreFromMenu();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
        exploreScreen.pullToRefresh();
        assertEquals("Invalid number of trending music items", 15, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTendingMusicLoadsNextPage(){
        exploreScreen.openExploreFromMenu();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional trending tracks in list", 25, exploreScreen.getItemsOnTrendingMusicList());
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.swipeLeftToTrendingAudio();
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
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional trending tracks in list", 25, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testGenresAreDisplayedUsingSwiping() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.swipeRightToGenres();
        assertEquals("Invalid number of genres found", 22, exploreScreen.getNumberOfItemsInGenresTab());
    }

    public void testGenresAreDisplayedWhenTouchingTab() {
        exploreScreen.openExploreFromMenu();
        exploreScreen.touchGenresTab();
        assertEquals("Invalid number of genres found", 22, exploreScreen.getNumberOfItemsInGenresTab());
    }


    @Override
    protected void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}