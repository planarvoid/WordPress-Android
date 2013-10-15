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
        discoveryScreen.swipeRight();
        assertEquals("Could not get to genres section", "GENRES", discoveryScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 22, discoveryScreen.getNumberOfItemsInGenresTab());
    }

    public void testGenresAreDisplayedWhenTouchingTab() {
        menuScreen.openExplore();
        discoveryScreen.touchGenresTab();
        assertEquals("Could not get to genres section", "GENRES", discoveryScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 22, discoveryScreen.getNumberOfItemsInGenresTab());
    }

    public void testTrendingAudioIsDisplayedUsingSwiping() {
        menuScreen.openExplore();
        discoveryScreen.swipeLeft();
        assertEquals("Could not get to genres section", "TRENDING AUDIO", discoveryScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 15, discoveryCategoryTracksScreen.getItemsOnList());
    }

    public void testTrendingAudioIsDisplayedWhenTouchingTab() {
        menuScreen.openExplore();
        discoveryScreen.touchTrendingAudioTab();
        assertEquals("Could not get to genres section", "TRENDING AUDIO", discoveryScreen.currentTabTitle());
        assertEquals("Invalid number of genres found", 15, discoveryCategoryTracksScreen.getItemsOnList());
    }

    public void testElectronicMusicCategoryHasContent(){
        menuScreen.openExplore();
        discoveryScreen.touchGenresTab();
        discoveryScreen.clickElectronicGenre();
        assertEquals(15, discoveryCategoryTracksScreen.getItemsOnList());
        discoveryCategoryTracksScreen.pullToRefresh();
        assertEquals(15, discoveryCategoryTracksScreen.getItemsOnList());
//        discoveryCategoryTracksScreen.scrollDown();
//        assertTrue("There should be additional tracks on the list", discoveryCategoryTracksScreen.getItemsOnList() > 15);
    }


    public void testTabsShouldResetWhenVisitedFromMenu() {
        menuScreen.openExplore();
        discoveryScreen.touchTrendingAudioTab();
        assertEquals("Could not get to genres section", "TRENDING AUDIO", discoveryScreen.currentTabTitle());
        menuScreen.openStream();
        menuScreen.openExplore();
        assertEquals("Could not get to genres section", "TRENDING MUSIC", discoveryScreen.currentTabTitle());

    }

    @Override
    protected void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}