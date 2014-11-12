package com.soundcloud.android.explore.genres;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

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
        assertThat(categoryScreen.getItemsOnList(), is(greaterThan(0)));
        assertThat(categoryScreen.getItemsOnList(), is(lessThanOrEqualTo(20)));
    }

    public void testElectronicMusicCategoryPullToRefresh(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertThat(categoryScreen.getItemsOnList(), is(greaterThan(0)));
        assertThat(categoryScreen.getItemsOnList(), is(lessThanOrEqualTo(20)));
        exploreScreen.pullToRefresh();
        assertThat(categoryScreen.getItemsOnList(), is(greaterThan(0)));
        assertThat(categoryScreen.getItemsOnList(), is(lessThanOrEqualTo(20)));
    }

    public void testElectronicMusicCategoryLoadsNextPageOfTracks(){
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        waiter.waitForContentAndRetryIfLoadingFailed();
        int numberOfTracks = categoryScreen.getItemsOnList();
        assertThat(numberOfTracks, is(greaterThan(0)));
        assertThat(numberOfTracks, is(lessThanOrEqualTo(20)));

        categoryScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertThat(categoryScreen.getItemsOnList(), is(greaterThan(numberOfTracks)));
    }
}
