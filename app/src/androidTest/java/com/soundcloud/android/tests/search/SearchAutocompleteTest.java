package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.TestUser.autocompleteTestUser;
import static com.soundcloud.android.properties.FeatureFlagsHelper.create;
import static com.soundcloud.android.properties.Flag.NEW_HOME;
import static com.soundcloud.android.properties.Flag.SEARCH_TOP_RESULTS;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.screens.FollowingsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import java.util.HashMap;

public class SearchAutocompleteTest extends ActivityTest<MainActivity> {

    private static final String SEARCH_AUTOCOMPLETE = "specs/search_autocomplete2.spec";
    private static final String SEARCH_AUTOCOMPLETE_ARROW = "specs/search_autocomplete_arrow.spec";
    private DiscoveryScreen discoveryScreen;

    public SearchAutocompleteTest() {
        super(MainActivity.class);
    }

    @Override
    protected void beforeActivityLaunched() {
        FeatureFlagsHelper helper = create(getInstrumentation().getTargetContext());
        helper.disable(SEARCH_TOP_RESULTS);
        helper.enable(NEW_HOME);
    }

    @Override
    protected TestUser getUserForLogin() {
        return autocompleteTestUser;
    }

    @Test
    public void testAutocompleteResults() throws Exception {
        final String firstSearchTerm = "beyo";
        final String secondSearchTerm = "a";

        // In order to guarantee the followings exist in the DB
        FollowingsScreen followingsScreen = mainNavHelper.goToMyProfile().touchInfoTab().clickFollowingsLink();
        followingsScreen.pullToRefresh();

        followingsScreen.goBackToProfile().goBack();

        discoveryScreen = mainNavHelper.goToDiscovery();

        mrLocalLocal.startEventTracking();

        SearchScreen searchScreen = discoveryScreen.clickSearch()
                                                   .setSearchQuery(firstSearchTerm);
        final String firstSuggestionQuery = searchScreen.firstAutocompleteSuggestionText();
        SearchResultsScreen resultsScreen = searchScreen.clickOnFirstAutocompleteSuggestion();

        assertThat("Search results screen should be visible", resultsScreen.isVisible());

        final ProfileScreen profileScreen = resultsScreen.clickSearch()
                                                         .setSearchQuery(secondSearchTerm)
                                                         .clickOnUserSuggestion();

        assertThat("Profile should be visible", profileScreen.isVisible());

        HashMap<String, String> stringSubstitutions = new HashMap<>(3);
        stringSubstitutions.put("\\$first_user_query\\$", firstSearchTerm);
        stringSubstitutions.put("\\$selected_search_term\\$", firstSuggestionQuery);
        stringSubstitutions.put("\\$second_user_query\\$", secondSearchTerm);
        mrLocalLocal.verify(SEARCH_AUTOCOMPLETE, stringSubstitutions);
    }

    @Test
    public void testAutocompleteArrowAndExit() throws Exception {
        discoveryScreen = mainNavHelper.goToDiscovery();
        final String searchTerm = "beyo";

        mrLocalLocal.startEventTracking();
        SearchScreen searchScreen = discoveryScreen.clickSearch()
                                                   .setSearchQuery(searchTerm);

        final String firstSuggestionQuery = searchScreen.firstAutocompleteSuggestionText();

        searchScreen.clickOnFirstAutocompleteSuggestionArrow();

        assertThat("Search screen should be visible", searchScreen.isVisible());
        assertEquals(firstSuggestionQuery, searchScreen.getSearchQuery());

        searchScreen.dismissSearch();

        HashMap<String, String> stringSubstitutions = new HashMap<>(2);
        stringSubstitutions.put("\\$user_query\\$", searchTerm);
        stringSubstitutions.put("\\$selected_search_term\\$", firstSuggestionQuery);
        mrLocalLocal.verify(SEARCH_AUTOCOMPLETE_ARROW, stringSubstitutions);
    }
}
