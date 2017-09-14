package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.discovery.SearchResultsTest.QUERY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.discovery.SearchResultsTest;
import org.junit.Test;

public class SearchNavigationTest extends ActivityTest<MainActivity> {

    private static final String SEARCH_LOCAL_RESULTS = "specs/search_local_results.spec";
    private DiscoveryScreen discoveryScreen;

    public SearchNavigationTest() {
        super(MainActivity.class);
    }


    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    @Ignore
    @Test
    public void testVerifySearchNavigation() throws Exception {
        assertGoBackFromSearchResultsReturnsToDiscoveryScreen();
        assertClickSearchSuggestionUser();
        assertClickSearchSuggestionTrack();
        assertClickSearchSuggestionPlaylist();
    }

    private void assertGoBackFromSearchResultsReturnsToDiscoveryScreen() {
        final SearchResultsScreen resultsScreen = discoveryScreen.clickSearch()
                                                                 .doSearch(QUERY);
        final DiscoveryScreen discoveryScreen = resultsScreen.goBack(DiscoveryScreen::new);

        assertThat("Tags screen should be visible", discoveryScreen, is(visible()));
    }

    private void assertClickSearchSuggestionUser() throws Exception {
        mrLocalLocal.startEventTracking();
        ProfileScreen profile = discoveryScreen.clickSearch()
                                               .setSearchQuery("skrillex")
                                               .clickOnUserSuggestion();

        assertThat("Profile screen should be visible", profile, is(visible()));

        mrLocalLocal.verify(SEARCH_LOCAL_RESULTS);
        solo.goBack();
        solo.goBack();
    }

    private void assertClickSearchSuggestionTrack() {
        VisualPlayerElement player = discoveryScreen.clickSearch()
                                                    .setSearchQuery("skrillex")
                                                    .clickOnTrackSuggestion();

        assertThat("Player should be visible", player.isVisible());
        player.clickArtwork();

        solo.goBack();
        solo.goBack();
    }

    private void assertClickSearchSuggestionPlaylist() {
        PlaylistDetailsScreen player = discoveryScreen.clickSearch()
                                                      .setSearchQuery("do not delete")
                                                      .clickOnPlaylistSuggestion();

        assertThat("Playlist details should be visible", player.isVisible());

        solo.goBack();
        solo.goBack();
    }
}
