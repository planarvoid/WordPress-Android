package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.TestUser.searchUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class SearchResultsTest extends ActivityTest<MainActivity> {
    private static final String ALBUMS_IN_SEARCH = "specs/albums_in_search2.spec";
    public static final String QUERY = "forss";

    private DiscoveryScreen discoveryScreen;

    public SearchResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return searchUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    @Test
    public void testSubmittingSearchQueryOpensSearchResults() throws Exception {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch(QUERY);

        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search results should be populated", resultsScreen.getResultItemCount(), is(greaterThan(0)));
    }

    @Test
    public void testGoingBackFromPlayingTrackFromSearchResultCollapsesThePlayer() throws Exception {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch("track");
        VisualPlayerElement playerElement = resultsScreen.findAndClickFirstTrackItem().pressBackToCollapse();

        assertThat("Player is collapsed", playerElement.isCollapsed());
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
    }

    @Test
    public void testTappingTrackOnAllTabOpensPlayer() throws Exception {
        VisualPlayerElement playerScreen = discoveryScreen.clickSearch().doSearch("track").findAndClickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    @Test
    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() throws Exception {
        PlaylistDetailsScreen playlistScreen = discoveryScreen.clickSearch()
                                                              .doSearch("track playlist")
                                                              .findAndClickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistScreen, is(visible()));
    }

    @Test
    public void testTappingUserOnAllTabOpensProfile() throws Exception {
        ProfileScreen profileScreen = discoveryScreen.clickSearch().doSearch("emptyuser").findAndClickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen, is(visible()));
    }

    @Test
    public void testTappingUserOnPeopleTabOpensProfile() throws Exception {
        ProfileScreen profileScreen = discoveryScreen.clickSearch()
                                                     .doSearch("emptyuser")
                                                     .goToPeopleTab()
                                                     .findAndClickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen, is(visible()));
    }

    @Test
    public void testTappingTrackOnTracksTabOpensPlayer() throws Exception {
        VisualPlayerElement playerScreen = discoveryScreen.clickSearch()
                                                          .doSearch(QUERY)
                                                          .goToTracksTab()
                                                          .findAndClickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    @Test
    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() throws Exception {
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

    @Test
    public void testTappingAlbumOnAlbumsTabOpensAlbumDetails() throws Exception {
        mrLocalLocal.startEventTracking();

        PlaylistDetailsScreen playlistDetailsScreen = discoveryScreen
                .clickSearch()
                .doSearch(QUERY)
                .goToAlbumsTab()
                .findAndClickFirstAlbumItem();

        assertThat("Album screen should be visible", playlistDetailsScreen, is(visible()));

        mrLocalLocal.verify(ALBUMS_IN_SEARCH);
    }

    @Test
    public void testOrderOfDisplayedTabsWithAlbums() throws Exception {
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

    @Test
    public void testAllResultsLoadsNextPage() throws Exception {
        SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch(QUERY);
        int initialItemCount = resultsScreen.getResultItemCount();
        resultsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(initialItemCount, is(lessThan(resultsScreen.getResultItemCount())));
    }

    @Test
    public void testShowSearchSuggestions() throws Exception {
        final boolean hasSearchResults = discoveryScreen.clickSearch().setSearchQuery("hello").hasSearchResults();
        assertThat("Should has suggestions", hasSearchResults, is(true));
    }

    @Test
    public void testDismissingSearchClearsUpSearchResults() throws Exception {
        SearchScreen searchScreen = discoveryScreen.clickSearch();
        searchScreen.setSearchQuery(QUERY).dismissSearch();

        assertThat("Search query should be empty", searchScreen.getSearchQuery(), isEmptyString());
        assertThat("Search results should be empty", searchScreen.hasSearchResults(), is(false));
    }
}
