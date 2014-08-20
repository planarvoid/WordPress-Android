package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.utils.Log;

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
        assertThat(exploreScreen.currentTabTitle(), is(equalTo("MUSIC")));
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(equalTo(20)));
    }

    public void testTrendingMusicPullToRefresh() {
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(equalTo(20)));

        exploreScreen.pullToRefresh();
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(equalTo(20)));
    }

    public void testTendingMusicLoadsNextPage(){
        exploreScreen.touchTrendingMusicTab();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(greaterThanOrEqualTo(20)));
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        exploreScreen.swipeLeft();

        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(exploreScreen.currentTabTitle(), is(equalTo("AUDIO")));
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(equalTo(20)));
    }

    public void testTrendingAudioIsDisplayedWhenTouchingTab() {
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(equalTo(20)));
    }

    public void testTrendingAudioPullToRefresh() {
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(equalTo(20)));
        assertEquals("Invalid number of trending audio items", 20, exploreScreen.getItemsOnTrendingAudioList());

        exploreScreen.pullToRefresh();
        assertEquals("Invalid number of trending audio items", 20, exploreScreen.getItemsOnTrendingAudioList());
    }

    public void testTendingAudioLoadsNextPage(){
        exploreScreen.touchTrendingAudioTab();
        int exploreTracksCountBefore = exploreScreen.getItemsOnTrendingAudioList();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertThat(exploreTracksCountBefore, is(lessThan(exploreScreen.getItemsOnTrendingAudioList())));

    }

    private ExploreScreen openExploreFromMenu() {
        return menuScreen.open()
                .clickExplore();
    }
}