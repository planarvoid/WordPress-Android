package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.explore.ExploreTracksScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

public class Categories extends ActivityTestCase<Home> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;
    private ExploreTracksScreen exploreTracksScreen;

    public Categories() {
        super(Home.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        waiter.waitForListContent();

        exploreScreen = new ExploreScreen(solo);
        exploreTracksScreen = new ExploreTracksScreen(solo);
    }

    public void testCategoriesHaveContent() {
        menuScreen.openExplore();
        exploreScreen.clickCategoriesTab();
        assertEquals(true, exploreScreen.hasMusicSection());
        assertEquals(true, exploreScreen.hasAudioSection());
    }

    public void testCategoryContent(){
        menuScreen.openExplore();
        exploreScreen.clickCategoriesTab();
        exploreScreen.clickCategory(3);
        assertEquals(15, exploreTracksScreen.getItemsOnList());
        exploreTracksScreen.pullToRefresh();
        assertEquals(15, exploreTracksScreen.getItemsOnList());
        exploreTracksScreen.scrollDown();
        assertTrue("There should be additional tracks on the list", exploreTracksScreen.getItemsOnList() > 15);
    }

    public void testPopularAudioHasContent() {
        menuScreen.openExplore();
        exploreScreen.clickPopularAudioTab();
        assertEquals(15, exploreScreen.getItemsOnList());
    }

    public void testTabsShouldResetWhenVisitedFromMenu() {
        menuScreen.openExplore();
        exploreScreen.clickPopularAudioTab();
        menuScreen.openStream();
        menuScreen.openExplore();
        assertEquals("Popular music", exploreScreen.getActiveTabName());
    }

    public void testHeaderReflectsCategoryName() {
        menuScreen.openExplore();
        exploreScreen.clickCategoriesTab();
        exploreScreen.clickCategory(1);
        assertEquals("Classical", exploreTracksScreen.getTitle());
    }

    @Override
    protected void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}