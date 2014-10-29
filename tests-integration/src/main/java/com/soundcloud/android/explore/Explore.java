package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

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
        AccountAssistant.loginAs(getInstrumentation(), testUser.getPermalink(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        menuScreen = new MenuScreen(solo);
        exploreScreen = openExploreFromMenu();
    }

    public void testTrendingMusicIsDisplayed() {
        assertThat(exploreScreen.currentTabTitle(), is(equalTo("MUSIC")));
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(greaterThan(0)));
    }

    public void testTrendingMusicPullToRefresh() {
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(greaterThan(0)));

        exploreScreen.pullToRefresh();
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(greaterThan(0)));
    }

    public void testTendingMusicLoadsNextPage(){
        exploreScreen.touchTrendingMusicTab();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertThat(exploreScreen.getItemsOnTrendingMusicList(), is(greaterThan(0)));
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        exploreScreen.swipeLeft();

        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(exploreScreen.currentTabTitle(), is(equalTo("AUDIO")));
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(greaterThan(0)));
    }

    public void testTrendingAudioIsDisplayedWhenTouchingTab() {
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(greaterThan(0)));
    }

    public void testTrendingAudioPullToRefresh() {
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(greaterThan(0)));

        exploreScreen.pullToRefresh();
        assertThat(exploreScreen.getItemsOnTrendingAudioList(), is(greaterThan(0)));
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