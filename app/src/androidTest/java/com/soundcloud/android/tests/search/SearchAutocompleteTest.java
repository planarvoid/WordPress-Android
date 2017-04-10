package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.FollowingsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class SearchAutocompleteTest extends TrackingActivityTest<MainActivity> {

    private static final String SEARCH_AUTOCOMPLETE = "search_autocomplete2";
    private DiscoveryScreen discoveryScreen;

    public SearchAutocompleteTest() {
        super(MainActivity.class);
    }

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.SEARCH_TOP_RESULTS);
    }


    @Override
    protected TestUser getUserForLogin() {
        return TestUser.autocompleteTestUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // In order to guarantee the followings exist in the DB
        FollowingsScreen followingsScreen = mainNavHelper.goToMyProfile().touchInfoTab().clickFollowingsLink();
        followingsScreen.pullToRefresh();

        followingsScreen.goBackToProfile().goBack();

        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testAutocompleteResults() throws Exception {
        startEventTracking();
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch()
                                                           .setSearchQuery("clown")
                                                           .clickOnAutocompleteSuggestion("clowns");

        assertThat("Search results screen should be visible", resultsScreen.isVisible());

        final ProfileScreen profileScreen = resultsScreen.clickSearch().setSearchQuery("a").clickOnUserSuggestion();

        assertThat("Profile should be visible", profileScreen.isVisible());

        finishEventTracking(SEARCH_AUTOCOMPLETE);
    }
}
