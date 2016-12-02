package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class SearchAutocompleteTest extends TrackingActivityTest<MainActivity> {

    private static final String SEARCH_AUTOCOMPLETE = "search_autocomplete";
    private DiscoveryScreen discoveryScreen;
    private FeatureFlagsHelper featureFlagsHelper;
    private boolean wasAutocompleteFlagEnabled;

    public SearchAutocompleteTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        featureFlagsHelper = FeatureFlagsHelper.create(getInstrumentation().getTargetContext());
        wasAutocompleteFlagEnabled = featureFlagsHelper.isEnabled(Flag.AUTOCOMPLETE);
        featureFlagsHelper.enable(Flag.AUTOCOMPLETE);

        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        featureFlagsHelper.setFlag(Flag.AUTOCOMPLETE, wasAutocompleteFlagEnabled);
    }

    public void testAutocompleteResults() throws Exception {
        startEventTracking();
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch()
                .setSearchQuery("clown")
                .clickOnAutocompleteSuggestion();

        assertThat("Search results screen should be visible", resultsScreen.isVisible());
        finishEventTracking(SEARCH_AUTOCOMPLETE);
    }
}
