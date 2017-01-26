package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class OldSearchSuggestionsTest extends TrackingActivityTest<MainActivity> {

    private static final String SEARCH_SUGGESTIONS = "search_suggestions";
    private static final String SEARCH_SUGGESTIONS_ITEM_NAV = "search_suggestions_item_nav";
    private DiscoveryScreen discoveryScreen;

    public OldSearchSuggestionsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
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

        finishEventTracking(SEARCH_SUGGESTIONS);
    }

    public void testSearchSuggestionsItemNavigation() throws Exception {
        startEventTracking();

        ProfileScreen profileScreen = discoveryScreen.clickSearch().setSearchQuery("annoymouse").clickOnUserSuggestion();

        assertThat("Search results screen should be visible", profileScreen.isVisible());

        finishEventTracking(SEARCH_SUGGESTIONS_ITEM_NAV);
    }
}
