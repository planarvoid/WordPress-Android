package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class SearchResultsTest extends TrackingActivityTest<MainActivity> {
    private static final String ALBUMS_IN_SEARCH = "albums_in_search2";
    public static final String QUERY = "forss";

    private DiscoveryScreen discoveryScreen;

    public SearchResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.SEARCH_TOP_RESULTS);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testSubmittingSearchQueryOpensSearchResults() {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch(QUERY);

        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search results should be populated", resultsScreen.getResultItemCount(), is(greaterThan(0)));
    }

    public void testGoingBackFromPlayingTrackFromSearchResultCollapsesThePlayer() {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch("track");
        VisualPlayerElement playerElement = resultsScreen.findAndClickFirstTrackItem().pressBackToCollapse();

        assertThat("Player is collapsed", playerElement.isCollapsed());
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
    }

    public void testTappingTrackOnAllTabOpensPlayer() {
        VisualPlayerElement playerScreen = discoveryScreen.clickSearch().doSearch("track").findAndClickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() {
        PlaylistDetailsScreen playlistScreen = discoveryScreen.clickSearch()
                                                              .doSearch("track playlist")
                                                              .findAndClickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistScreen, is(visible()));
    }

    public void testTappingUserOnAllTabOpensProfile() {
        ProfileScreen profileScreen = discoveryScreen.clickSearch().doSearch("emptyuser").findAndClickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen, is(visible()));
    }

    public void testTappingUserOnPeopleTabOpensProfile() {
        ProfileScreen profileScreen = discoveryScreen.clickSearch()
                                                     .doSearch("emptyuser")
                                                     .goToPeopleTab()
                                                     .findAndClickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen, is(visible()));
    }

    public void testTappingTrackOnTracksTabOpensPlayer() {
        VisualPlayerElement playerScreen = discoveryScreen.clickSearch()
                                                          .doSearch(QUERY)
                                                          .goToTracksTab()
                                                          .findAndClickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = discoveryScreen
                .clickSearch()
                .doSearch(QUERY)
                .goToPlaylistsTab()
                .findAndClickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistDetailsScreen, is(visible()));
        assertThat("Playlist screen title should be 'Playlist'",
                   playlistDetailsScreen.getActionBarTitle(),
                   equalTo("Playlist"));
    }

    public void testTappingAlbumOnAlbumsTabOpensAlbumDetails() {
        startEventTracking();

        PlaylistDetailsScreen playlistDetailsScreen = discoveryScreen
                .clickSearch()
                .doSearch(QUERY)
                .goToAlbumsTab()
                .findAndClickFirstAlbumItem();

        assertThat("Album screen should be visible", playlistDetailsScreen, is(visible()));

        finishEventTracking(ALBUMS_IN_SEARCH);
    }

    public void testOrderOfDisplayedTabsWithAlbums() {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch(QUERY);
        assertThat("Current tab should be ALL", resultsScreen.currentTabTitle(), is("ALL"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be TRACKS", resultsScreen.currentTabTitle(), is("TRACKS"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be PEOPLE", resultsScreen.currentTabTitle(), is("PEOPLE"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be ALBUMS", resultsScreen.currentTabTitle(), is("ALBUMS"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be PLAYLISTS", resultsScreen.currentTabTitle(), is("PLAYLISTS"));
    }

    public void testAllResultsLoadsNextPage() {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch(QUERY);
        int initialItemCount = resultsScreen.getResultItemCount();
        resultsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(initialItemCount, is(lessThan(resultsScreen.getResultItemCount())));
    }

    public void testShowSearchSuggestions() {
        final boolean hasSearchResults = discoveryScreen.clickSearch().setSearchQuery("hello").hasSearchResults();
        assertThat("Should has suggestions", hasSearchResults, is(true));
    }

    public void testDismissingSearchClearsUpSearchResults() {
        SearchScreen searchScreen = discoveryScreen.clickSearch();
        searchScreen.setSearchQuery(QUERY).dismissSearch();

        assertThat("Search query should be empty", searchScreen.getSearchQuery(), isEmptyString());
        assertThat("Search results should be empty", searchScreen.hasSearchResults(), is(false));
    }
}
