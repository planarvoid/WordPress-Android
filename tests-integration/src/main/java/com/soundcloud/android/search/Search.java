package com.soundcloud.android.search;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

public class Search extends ActivityTestCase<MainActivity> {

    private MainScreen mainScreen;
    private PlaylistTagsScreen playlistTagsScreen;

    public Search() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        super.setUp();

        mainScreen = new MainScreen(solo);
        playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
    }

    public void testTappingSearchIconOpensFullPageSearch() {
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    public void testClickingPhysicalSearchButtonOpensFullPageSearch() {
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    public void testSubmittingSearchQueryOpensSearchResults() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testSubmittingSearchQueryRendersSearchResultsList() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        assertTrue("Search results should be populated", resultsScreen.getResultItemCount() > 0);
    }

    public void testGoingBackFromSearchResultsReturnsToPreviousPage() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        MainScreen mainScreen = resultsScreen.pressBack();
        assertEquals("Main screen should be visible", true, mainScreen.isVisible());
    }

    public void testSearchingFromSuggestionShortcutShowsSearchResults() {
        playlistTagsScreen.actionBar().setSearchQuery("dubstep");
        solo.clickInList(0);

        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testClearingSearchFieldReturnsToDisplayingTags() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().dismissSearch();
        assertEquals("Playlist tags screen should be visible", true, tagsScreen.isVisible());
    }

    public void testTappingTrackOnAllTabOpensPlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track");
        PlayerScreen playerScreen = resultsScreen.clickFirstTrackItem();
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track playlist");
        PlaylistDetailsScreen playlistScreen = resultsScreen.clickFirstPlaylistItem();
        assertEquals("Playlist screen should be visible", true, playlistScreen.isVisible());
    }

    public void testTappingUserOnAllTabOpensProfile() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        ProfileScreen profileScreen = resultsScreen.clickFirstUserItem();
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

    public void testTappingTrackOnTracksTabOpensPlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        resultsScreen.touchTracksTab();
        PlayerScreen playerScreen = resultsScreen.clickFirstTrackItem();
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        resultsScreen.touchPlaylistsTab();
        PlaylistDetailsScreen playlistScreen = resultsScreen.clickFirstPlaylistItem();
        assertEquals("Playlist screen should be visible", true, playlistScreen.isVisible());
    }

    public void testTappingUserOnPeopleTabOpensProfile() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        resultsScreen.touchPeopleTab();
        ProfileScreen profileScreen = resultsScreen.clickFirstUserItem();
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

    public void testOrderOfDisplayededTabs() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        assertEquals("Current tab should be ALL", "ALL", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be TRACKS", "TRACKS", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be PLAYLISTS", "PLAYLISTS", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be PEOPLE", "PEOPLE", resultsScreen.currentTabTitle());
    }

    public void testAllResultsLoadsNextPage(){
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        int initialItemCount = resultsScreen.getResultItemCount();
        resultsScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertTrue(initialItemCount < resultsScreen.getResultItemCount());
    }

}
