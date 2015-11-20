package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BrokenSearchTest;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.search.LegacySearchActivity;
import com.soundcloud.android.tests.ActivityTest;

public class SearchTest extends ActivityTest<LegacySearchActivity> {
    private PlaylistTagsScreen playlistTagsScreen;

    public SearchTest() {
        super(LegacySearchActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        StreamScreen streamScreen = new StreamScreen(solo);
        playlistTagsScreen = streamScreen.actionBar().clickSearchButton();
    }

    @BrokenSearchTest
    public void testClickingPhysicalSearchButtonOpensFullPageSearch() {
        assertEquals("Playlist tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

    @BrokenSearchTest
    public void testSubmittingSearchQueryOpensSearchResults() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");

        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
        assertThat("Search results should be populated", resultsScreen.getResultItemCount(), is(greaterThan(0)));
    }

    @BrokenSearchTest
    public void testGoingBackFromPlayingTrackFromSearchResultCollapsesThePlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track");
        resultsScreen.clickFirstTrackItem();
        VisualPlayerElement playerElement = new VisualPlayerElement(solo);
        playerElement.pressBackToCollapse();

        assertThat(playerElement, is(collapsed()));
        assertThat(resultsScreen, is(visible()));
    }

    @BrokenSearchTest
    public void testSearchingFromSuggestionShortcutShowsSearchResults() {
        playlistTagsScreen.actionBar().setSearchQuery("dubstep");
        //TODO: That should actually be handled buy SearchSuggestionsElement class
        solo.clickOnText("Search for 'dubstep'");

        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

    @BrokenSearchTest
    public void testClearingSearchFieldReturnsToDisplayingTags() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().dismissSearch();
        assertEquals("Playlist tags screen should be visible", true, tagsScreen.isVisible());
    }

    @BrokenSearchTest
    public void testTappingTrackOnAllTabOpensPlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track");
        resultsScreen.clickFirstTrackItem();
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    @BrokenSearchTest
    public void testTappingPlaylistOnAllTabOpensPlaylistDetails() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("track playlist");
        resultsScreen.clickFirstPlaylistItem();
        PlaylistDetailsScreen playlistScreen = new PlaylistDetailsScreen(solo);
        assertEquals("Playlist screen should be visible", true, playlistScreen.isVisible());
    }

    @BrokenSearchTest
    public void testTappingUserOnAllTabOpensProfile() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.clickFirstUserItem();
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

    @BrokenSearchTest
    public void testTappingTrackOnTracksTabOpensPlayer() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.touchTracksTab();
        resultsScreen.clickFirstTrackItem();
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        assertEquals("Player screen should be visible", true, playerScreen.isVisible());
    }

    @BrokenSearchTest
    public void testTappingPlaylistOnPlaylistsTabOpensPlaylistDetails() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.touchPlaylistsTab();
        resultsScreen.clickFirstPlaylistItem();
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);
        assertEquals("Playlist screen should be visible", true, playlistDetailsScreen.isVisible());
    }

    @BrokenSearchTest
    public void testTappingUserOnPeopleTabOpensProfile() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.touchPeopleTab();
        resultsScreen.clickFirstUserItem();
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertEquals("Profile screen should be visible", true, profileScreen.isVisible());
    }

    @BrokenSearchTest
    public void testOrderOfDisplayededTabs() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        assertEquals("Current tab should be ALL", "ALL", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be TRACKS", "TRACKS", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be PLAYLISTS", "PLAYLISTS", resultsScreen.currentTabTitle());
        resultsScreen.swipeLeft();
        assertEquals("Current tab should be PEOPLE", "PEOPLE", resultsScreen.currentTabTitle());
    }

    @BrokenSearchTest
    public void testAllResultsLoadsNextPage() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        int initialItemCount = resultsScreen.getResultItemCount();
        resultsScreen.scrollToBottomOfTracksListAndLoadMoreItems();
        assertTrue(initialItemCount < resultsScreen.getResultItemCount());
    }

    @BrokenSearchTest
    public void testShowKeyboardWhenEnteringSearch() {
        assertEquals("Keyboard should be visible when entering search", true, playlistTagsScreen.isKeyboardShown());
    }

    @BrokenSearchTest
    public void testShouldHideSoftKeyboardWhenScrollingTagsVertically() {
        playlistTagsScreen.scrollDown();
        assertEquals("Keyboard should be hidden when scrolling", false, playlistTagsScreen.isKeyboardShown());
    }

    @BrokenSearchTest
    public void testShouldFollowUser() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("andtestpl");

        UserItemElement user = resultsScreen.touchAllTab().getFirstUser();
        final String username = user.getUsername();
        boolean wasFollowing = user.isFollowing();

        user.toggleFollow();
        assertThat("Should change following state",
                wasFollowing, is(not(equalTo(user.isFollowing()))));

        user = resultsScreen.touchPeopleTab().scrollToUserWithUsername(username);
        assertThat("Should keep changed following state when switching tabs",
                wasFollowing, is(not(equalTo(user.isFollowing()))));
    }
}
