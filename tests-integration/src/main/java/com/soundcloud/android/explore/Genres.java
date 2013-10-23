package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.explore.DiscoveryCategoryTracksScreen;
import com.soundcloud.android.screens.explore.DiscoveryScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

public class Genres extends ActivityTestCase<Home> {
    private Waiter waiter;
    private DiscoveryScreen discoveryScreen;
    private DiscoveryCategoryTracksScreen discoveryCategoryTracksScreen;

    public Genres() {
        super(Home.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        waiter.waitForListContent();

        discoveryScreen = new DiscoveryScreen(solo, waiter, this);
        discoveryCategoryTracksScreen = new DiscoveryCategoryTracksScreen(solo);
    }

    public void testGenresAreDisplayedUsingSwiping() {
        menuScreen.openExplore();
        discoveryScreen.swipeRightToGenres();
        assertEquals("Invalid number of genres found", 22, discoveryScreen.getNumberOfItemsInGenresTab());
    }

    public void testGenresAreDisplayedWhenTouchingTab() {
        menuScreen.openExplore();
        discoveryScreen.touchGenresTab();
        assertEquals("Invalid number of genres found", 22, discoveryScreen.getNumberOfItemsInGenresTab());
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        menuScreen.openExplore();
        discoveryScreen.swipeLeftToTrendingAudio();
        assertEquals("Invalid number of genres found", 15, discoveryCategoryTracksScreen.getItemsOnList());
    }

    public void testTrendingAudioIsDisplayedWhenTouchingTab() {
        menuScreen.openExplore();
        discoveryScreen.touchTrendingAudioTab();
        assertEquals("Invalid number of genres found", 15, discoveryCategoryTracksScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryHasContent(){
        menuScreen.openExplore();
        discoveryScreen.touchGenresTab();
        discoveryScreen.clickElectronicGenre();
        assertEquals(15, discoveryCategoryTracksScreen.getItemsOnList());
        discoveryCategoryTracksScreen.pullToRefresh();
        assertEquals(15, discoveryCategoryTracksScreen.getItemsOnList());
        discoveryCategoryTracksScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertEquals("There should be additional tracks on the list", 25, discoveryCategoryTracksScreen.getItemsOnList());
    }


    @Override
    protected void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}