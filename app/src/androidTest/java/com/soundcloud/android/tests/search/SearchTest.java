package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SearchTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;
    private PlaylistTagsScreen playlistTagsScreen;

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

        streamScreen = new StreamScreen(solo);
        playlistTagsScreen = streamScreen.actionBar().clickSearchButton();
    }

    public void testTappingSearchIconOpensFullPageSearch() {
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    public void testClickingPhysicalSearchButtonOpensFullPageSearch() {
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    public void testSubmittingSearchQueryOpensSearchResults() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testSubmittingSearchQueryRendersSearchResultsList() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        assertTrue("Search results should be populated", resultsScreen.getResultItemCount() > 0);
    }

    public void testGoingBackFromSearchResultsReturnsToTagPage() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.pressBack();
        playlistTagsScreen = new PlaylistTagsScreen(solo);
        assertEquals("Tags screen should be visible", true, playlistTagsScreen.isVisible());
        assertEquals("Search query should be empty", "", playlistTagsScreen.actionBar().getSearchQuery());
    }

    public void testGoingBackFromTagsScreenExitsSearch() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.pressBack();
        playlistTagsScreen = new PlaylistTagsScreen(solo);
        streamScreen = playlistTagsScreen.pressBack();
        assertEquals("Main screen should be visible", true, streamScreen.isVisible());
    }

    public void testGoingBackFromPlayingTrackFromSearchResultCollapsesThePlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track");
        resultsScreen.clickFirstTrackItem();
        VisualPlayerElement playerElement = new VisualPlayerElement(solo);
        playerElement.pressBackToCollapse();

        assertThat(playerElement, is(collapsed()));
        assertThat(resultsScreen, is(visible()));
    }

    public void testSearchingFromSuggestionShortcutShowsSearchResults() {
        playlistTagsScreen.actionBar().setSearchQuery("dubstep");
        //TODO: That should actually be handled buy SearchSuggestionsElement class
        solo.clickOnText("Search for 'dubstep'");

        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testClearingSearchFieldReturnsToDisplayingTags() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().dismissSearch();
        assertEquals("Playlist tags screen should be visible", true, tagsScreen.isVisible());
    }

    public void testTappingTrackOnAllTabOpensPlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track");
        resultsScreen.clickFirstTrackItem();
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track playlist");
        resultsScreen.clickFirstPlaylistItem();
        PlaylistDetailsScreen playlistScreen = new PlaylistDetailsScreen(solo);
        assertEquals("Playlist screen should be visible", true, playlistScreen.isVisible());
    }

    public void testTappingUserOnAllTabOpensProfile() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.clickFirstUserItem();
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

    public void testTappingTrackOnTracksTabOpensPlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.touchTracksTab();
        resultsScreen.clickFirstTrackItem();
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.touchPlaylistsTab();
        resultsScreen.clickFirstPlaylistItem();
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);
        assertEquals("Playlist screen should be visible", true, playlistDetailsScreen.isVisible());
    }

    public void testTappingUserOnPeopleTabOpensProfile() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.touchPeopleTab();
        resultsScreen.clickFirstUserItem();
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

    public void testOrderOfDisplayededTabs() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertEquals("Current tab should be ALL", "ALL", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be TRACKS", "TRACKS", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be PLAYLISTS", "PLAYLISTS", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be PEOPLE", "PEOPLE", resultsScreen.currentTabTitle());
    }

    public void testAllResultsLoadsNextPage() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
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
