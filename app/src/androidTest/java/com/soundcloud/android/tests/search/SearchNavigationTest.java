package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SearchNavigationTest extends ActivityTest<MainActivity> {

    private DiscoveryScreen discoveryScreen;

    public SearchNavigationTest() {
        super(MainActivity.class);
    }


    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testVerifySearchNavigation() throws Exception {
        assertGoBackFromSearchResultsReturnsToDiscoveryScreen();
        assertClickSearchSuggestionTrack();
        assertClickSearchSuggestionUser();
        assertClickSearchSuggestionPlaylist();
    }

    private void assertGoBackFromSearchResultsReturnsToDiscoveryScreen() {
        final SearchResultsScreen resultsScreen = discoveryScreen.clickSearch()
                                                                 .doSearch("clownstep");
        final DiscoveryScreen discoveryScreen = resultsScreen.goBack();

        assertThat("Tags screen should be visible", discoveryScreen, is(visible()));
    }

    private void assertClickSearchSuggestionUser() {
        ProfileScreen profile = discoveryScreen.clickSearch()
                                               .setSearchQuery("skrillex")
                                               .clickOnUserSuggestion();

        assertThat("Profile screen should be visible", profile, is(visible()));
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
                                                      .setSearchQuery("test playlist")
                                                      .clickOnPlaylistSuggestion();

        assertThat("Playlist details should be visible", player.isVisible());

        solo.goBack();
        solo.goBack();
    }
}
