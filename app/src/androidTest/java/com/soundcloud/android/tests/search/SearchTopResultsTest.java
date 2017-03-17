package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchTopResultsScreen;

public class SearchTopResultsTest extends TrackingActivityTest<MainActivity> {

    private static final String SEARCH_TOP_RESULTS = "search_top_results";
    private DiscoveryScreen discoveryScreen;
//    private FeatureFlagsHelper featureFlagsHelper;

    public SearchTopResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.topResultsTestUser;
    }


//    @Override
//    protected void beforeStartActivity() {
//        super.beforeStartActivity();
//        featureFlagsHelper = FeatureFlagsHelper.create(getInstrumentation().getTargetContext());
//        featureFlagsHelper.enable(Flag.SEARCH_TOP_RESULTS);
//    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }
//
//    @Override
//    public void tearDown() throws Exception {
//        super.tearDown();
//        featureFlagsHelper.reset(Flag.SEARCH_TOP_RESULTS);
//    }

    public void testTopResults() throws Exception {
        //startEventTracking();
        SearchTopResultsScreen resultsScreen = discoveryScreen.clickSearch()
                                                              .doSearchTopResults("coldplay");

        assertThat("Search top results screen should be visible", resultsScreen.isVisible());

        assertTrue(resultsScreen.goTracksHeader().hasVisibility());


//        final ProfileScreen profileScreen = resultsScreen.clickSearch().setSearchQuery("a").clickOnUserSuggestion();
//
//        assertThat("Profile should be visible", profileScreen.isVisible());

        //finishEventTracking(SEARCH_TOP_RESULTS);
    }
}
