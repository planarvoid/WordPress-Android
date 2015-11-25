package com.soundcloud.android.tests.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SearchTest extends ActivityTest<MainActivity> {
    private SearchScreen searchScreen;

    public SearchTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        searchScreen = mainNavHelper.goToDiscovery().clickSearch();
    }

    public void testSubmittingSearchQueryOpensSearchResults() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("clownstep");

        assertThat("Search results screen should be visible", resultsScreen.isVisible());
        assertThat("Search results should be populated", resultsScreen.getResultItemCount(), is(greaterThan(0)));
    }

    public void testGoingBackFromPlayingTrackFromSearchResultCollapsesThePlayer() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("track");
        VisualPlayerElement playerElement = resultsScreen.clickFirstTrackItem().pressBackToCollapse();

        assertThat("Player is collapsed", playerElement.isCollapsed());
        assertThat("Search results screen should be visible", resultsScreen.isVisible());
    }

    @Ignore
    public void testSearchingFromSuggestionShortcutShowsSearchResults() {
        SearchResultsScreen resultsScreen = searchScreen.setSearchQuery("dubstep").clickOnCurrentSearchQuery();

        assertThat("Search results screen should be visible", resultsScreen.isVisible());
    }

    public void testTappingTrackOnAllTabOpensPlayer() {
        VisualPlayerElement playerScreen = searchScreen.doSearch("track").clickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() {
        PlaylistDetailsScreen playlistScreen = searchScreen.doSearch("track playlist").clickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistScreen.isVisible());
    }

    public void testTappingUserOnAllTabOpensProfile() {
        ProfileScreen profileScreen = searchScreen.doSearch("clownstep").clickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen.isVisible());
    }

    public void testTappingTrackOnTracksTabOpensPlayer() {
        VisualPlayerElement playerScreen = searchScreen.doSearch("clownstep").goToTracksTab().clickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    @Ignore
    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = searchScreen
                .setSearchQuery("clownstep")
                .clickOnCurrentSearchQuery()
                .goToPlaylistsTab()
                .clickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistDetailsScreen.isVisible());
    }

    public void testTappingUserOnPeopleTabOpensProfile() {
        ProfileScreen profileScreen = searchScreen.doSearch("clownstep").goToPeopleTab().clickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen.isVisible());
    }

    public void testOrderOfDisplayededTabs() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("clownstep");
        assertThat("Current tab should be ALL", resultsScreen.currentTabTitle(), is("ALL"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be TRACKS", resultsScreen.currentTabTitle(), is("TRACKS"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be PLAYLISTS", resultsScreen.currentTabTitle(), is("PLAYLISTS"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be PEOPLE", resultsScreen.currentTabTitle(), is("PEOPLE"));
    }

    public void testAllResultsLoadsNextPage() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("clownstep");
        int initialItemCount = resultsScreen.getResultItemCount();
        resultsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(initialItemCount, is(lessThan(resultsScreen.getResultItemCount())));
    }

    public void testDismissingSearchClearsUpSearchResults() {
        searchScreen.setSearchQuery("clownstep").dismissSearch();

        assertThat("Search query should be empty", searchScreen.getSearchQuery(), isEmptyString());
        assertThat("Search results should be empty", searchScreen.hasSearchResults(), is(false));
    }
}
