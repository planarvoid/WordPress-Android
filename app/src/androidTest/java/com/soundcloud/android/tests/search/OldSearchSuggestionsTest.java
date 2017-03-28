package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class OldSearchSuggestionsTest extends TrackingActivityTest<MainActivity> {

    private static final String SEARCH_SUGGESTIONS = "search_suggestions";
    private DiscoveryScreen discoveryScreen;

    public OldSearchSuggestionsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.autocompleteTestUser;
    }

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.AUTOCOMPLETE);
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.SEARCH_TOP_RESULTS);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testSearchSuggestions() throws Exception {
        startEventTracking();

        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch()
                                                           .doSearch("clown");

        assertThat("Search results screen should be visible", resultsScreen.isVisible());

        final ProfileScreen profileScreen = resultsScreen.clickSearch().setSearchQuery("a").clickOnUserSuggestion();

        assertThat("Profile should be visible", profileScreen.isVisible());

        finishEventTracking(SEARCH_SUGGESTIONS);
    }
}