package com.soundcloud.android.search;


import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.search.SearchPlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class Search extends ActivityTestCase<MainActivity> {

    private MainScreen mainScreen;

    public Search() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        mainScreen = new MainScreen(solo);
    }

    public void testTappingSearchIconOpensFullPageSearch() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    public void testClickingPhysicalSearchButtonOpensFullPageSearch() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.clickPhysicalSearchButton();
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    public void testSubmittingSearchQueryOpensSearchResults() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testSubmittingSearchQueryRendersSearchResultsList() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        assertTrue("Search results should be populated", resultsScreen.getResultItemCount() > 0);
    }

    public void testGoingBackFromSearchResultsReturnsToPreviousPage() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        MainScreen mainScreen = resultsScreen.pressBack();
        assertEquals("Main screen should be visible", true, mainScreen.isVisible());
    }

    public void testTypingSearchFieldPresentsSearchSuggestions() {
        // To be implemented
    }

    public void testClearingSearchFieldReturnsToDisplayingTags() {
        // To be implemented
    }

    public void testTappingTrackOpensPlayer() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track");
        PlayerScreen playerScreen = resultsScreen.clickFirstTrackItem();
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    public void testTappingPlaylistOpensPlaylistDetails() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track playlist");
        PlaylistDetailsScreen playlistScreen = resultsScreen.clickFirstPlaylistItem();
        assertEquals("Playlist screen should be visible", true, playlistScreen.isVisible());
    }

    public void testTappingUserOpensProfile() {
        SearchPlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("skrillex");
        ProfileScreen profileScreen = resultsScreen.clickFirstUserItem();
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

}
