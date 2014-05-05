package com.soundcloud.android.search;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class Search extends ActivityTestCase<MainActivity> {

    private MainScreen mainScreen;
    private PlaylistTagsScreen playlistTagsScreen;

    public Search() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

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

    public void testGoingBackFromSearchResultsReturnsToTagPage() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        playlistTagsScreen = resultsScreen.pressBack();
        assertEquals("Tags screen should be visible", true, playlistTagsScreen.isVisible());
        assertEquals("Search query should be empty", "", playlistTagsScreen.actionBar().getSearchQuery());
    }

    public void testGoingBackFromTagsScreenExitsSearch() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        playlistTagsScreen = resultsScreen.pressBack();
        mainScreen = playlistTagsScreen.pressBack();
        assertEquals("Main screen should be visible", true, mainScreen.isVisible());
    }

    public void testSearchingFromSuggestionShortcutShowsSearchResults() {
        playlistTagsScreen.actionBar().setSearchQuery("dubstep");
        //TODO: That should actually be handled buy SearchSuggestionsElement class
        solo.waitForText("Search for", 1, 1000, false);
        solo.clickOnText("Search for");

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
        LegacyPlayerScreen playerScreen = resultsScreen.clickFirstTrackItem();
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
        LegacyPlayerScreen playerScreen = resultsScreen.clickFirstTrackItem();
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
        waiter.waitForContentAndRetryIfLoadingFailed();
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

    public void testShowKeyboardWhenEnteringSearch() {
        assertEquals("Keyboard should be visible when entering search", true, playlistTagsScreen.isKeyboardShown());
    }

    public void testShouldHideSoftKeyboardWhenScrollingTagsVertically() {
        solo.getSolo().scrollDown();
        assertEquals("Keyboard should be hidden when scrolling", false, playlistTagsScreen.isKeyboardShown());
    }

}
