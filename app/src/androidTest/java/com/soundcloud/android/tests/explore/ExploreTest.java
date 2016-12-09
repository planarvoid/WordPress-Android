package com.soundcloud.android.tests.explore;

import static com.soundcloud.android.framework.TestUser.testUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ExploreTest extends ActivityTest<MainActivity> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;

    public ExploreTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return testUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        waiter = new Waiter(solo);
        exploreScreen = mainNavHelper.goToExplore();
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

    public void testTendingMusicLoadsNextPage() {
        exploreScreen.touchTrendingMusicTab();
        int exploreTracksCountBefore = exploreScreen.getItemsOnTrendingMusicList();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertThat(exploreTracksCountBefore, is(lessThan(exploreScreen.getItemsOnTrendingMusicList())));
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

    public void testTendingAudioLoadsNextPage() {
        exploreScreen.touchTrendingAudioTab();
        int exploreTracksCountBefore = exploreScreen.getItemsOnTrendingAudioList();
        exploreScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertThat(exploreTracksCountBefore, is(lessThan(exploreScreen.getItemsOnTrendingAudioList())));
    }

}
