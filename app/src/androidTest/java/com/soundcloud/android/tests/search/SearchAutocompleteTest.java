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

public class SearchAutocompleteTest extends TrackingActivityTest<MainActivity> {

    private static final String SEARCH_AUTOCOMPLETE = "search_autocomplete2";
    private static final String SEARCH_AUTOCOMPLETE_ITEM_NAV = "search_autocomplete_item_nav";
    private DiscoveryScreen discoveryScreen;
    private FeatureFlagsHelper featureFlagsHelper;

    public SearchAutocompleteTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        featureFlagsHelper = FeatureFlagsHelper.create(getInstrumentation().getTargetContext());
        featureFlagsHelper.enable(Flag.AUTOCOMPLETE);

        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        featureFlagsHelper.reset(Flag.AUTOCOMPLETE);
    }

    public void testAutocompleteResults() throws Exception {
        startEventTracking();
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch()
                                                           .setSearchQuery("clown")
                                                           .clickOnAutocompleteSuggestion();

        assertThat("Search results screen should be visible", resultsScreen.isVisible());
        finishEventTracking(SEARCH_AUTOCOMPLETE);
    }

    public void testAutocompleteItemNavigation() throws Exception {
        startEventTracking();

        ProfileScreen profileScreen = discoveryScreen.clickSearch().setSearchQuery("a").clickOnUserSuggestion();

        assertThat("Search results screen should be visible", profileScreen.isVisible());

        finishEventTracking(SEARCH_AUTOCOMPLETE_ITEM_NAV);
    }
}
