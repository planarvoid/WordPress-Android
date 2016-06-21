package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.AlbumsTest;
import com.soundcloud.android.framework.annotation.PreAlbumsTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SearchResultsTest extends ActivityTest<MainActivity> {
    private SearchScreen searchScreen;

    public SearchResultsTest() {
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

        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search results should be populated", resultsScreen.getResultItemCount(), is(greaterThan(0)));
    }

    public void testGoingBackFromPlayingTrackFromSearchResultCollapsesThePlayer() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("track");
        VisualPlayerElement playerElement = resultsScreen.findAndClickFirstTrackItem().pressBackToCollapse();

        assertThat("Player is collapsed", playerElement.isCollapsed());
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
    }

    public void testTappingTrackOnAllTabOpensPlayer() {
        VisualPlayerElement playerScreen = searchScreen.doSearch("track").findAndClickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() {
        PlaylistDetailsScreen playlistScreen = searchScreen.doSearch("track playlist").findAndClickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistScreen, is(visible()));
    }

    public void testTappingUserOnAllTabOpensProfile() {
        ProfileScreen profileScreen = searchScreen.doSearch("emptyuser").findAndClickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen, is(visible()));
    }

    public void testTappingUserOnPeopleTabOpensProfile() {
        ProfileScreen profileScreen = searchScreen.doSearch("emptyuser").goToPeopleTab().findAndClickFirstUserItem();

        assertThat("Profile screen should be visible", profileScreen, is(visible()));
    }

    public void testTappingTrackOnTracksTabOpensPlayer() {
        VisualPlayerElement playerScreen = searchScreen.doSearch("clownstep").goToTracksTab().findAndClickFirstTrackItem();

        assertThat("Player screen should be visible", playerScreen.isVisible());
    }

    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = searchScreen
                .doSearch("clownstep")
                .goToPlaylistsTab()
                .findAndClickFirstPlaylistItem();

        assertThat("Playlist screen should be visible", playlistDetailsScreen, is(visible()));
        assertThat("Playlist screen title should be 'Playlist'", playlistDetailsScreen.getActionBarTitle(), equalTo("Playlist"));
    }

    @AlbumsTest
    public void testTappingAlbumOnAlbumsTabOpensAlbumDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = searchScreen
                .doSearch("clownstep")
                .goToAlbumsTab()
                .findAndClickFirstAlbumItem();

        assertThat("Album screen should be visible", playlistDetailsScreen, is(visible()));
    }

    //TODO remove test when search of albums is released
    @PreAlbumsTest
    public void testOrderOfDisplayedTabs() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("clownstep");
        assertThat("Current tab should be ALL", resultsScreen.currentTabTitle(), is("ALL"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be TRACKS", resultsScreen.currentTabTitle(), is("TRACKS"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be PEOPLE", resultsScreen.currentTabTitle(), is("PEOPLE"));

        resultsScreen.swipeLeft();
        assertThat("Current tab should be PLAYLISTS", resultsScreen.currentTabTitle(), is("PLAYLISTS"));
    }

    @AlbumsTest
    public void testOrderOfDisplayedTabsWithAlbums() {
        SearchResultsScreen resultsScreen = searchScreen.doSearch("clownstep");
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
        SearchResultsScreen resultsScreen = searchScreen.doSearch("clownstep");
        int initialItemCount = resultsScreen.getResultItemCount();
        resultsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(initialItemCount, is(lessThan(resultsScreen.getResultItemCount())));
    }

    public void testShowSearchSuggestions() {
        assertThat("Should has suggestions", searchScreen.setSearchQuery("hello").hasSearchResults(), is(true));
    }

    public void testDismissingSearchClearsUpSearchResults() {
        searchScreen.setSearchQuery("clownstep").dismissSearch();

        assertThat("Search query should be empty", searchScreen.getSearchQuery(), isEmptyString());
        assertThat("Search results should be empty", searchScreen.hasSearchResults(), is(false));
    }
}
