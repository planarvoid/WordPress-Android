package com.soundcloud.android.search;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

import java.util.List;

public class PlaylistDiscovery extends ActivityTestCase<MainActivity> {

    private MainScreen mainScreen;
    private PlaylistTagsScreen playlistTagsScreen;

    public PlaylistDiscovery() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        super.setUp();

        mainScreen = new MainScreen(solo);
        playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
    }

    public void testTagsAreDisplayedWhenSearchScreenIsOpened() {
        assertEquals("Playlist tags should be visible", true, playlistTagsScreen.isDisplayingTags());
    }

    public void testTagDisplayedAsSuggestionAfterTagSearch() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.actionBar().doTagSearch("#deep house");
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().dismissSearch();

        assertEquals("Playlist tags screen should be visible", true, tagsScreen.isVisible());
        assertEquals("Searched tag should be in recents", true, tagsScreen.getRecentTags().contains("#deep house"));
    }

    public void testClickingOnPlaylistTagOpensPlaylistResultsScreenWith20Results() {
        playlistTagsScreen.clickOnTag(0);

        PlaylistResultsScreen resultsScreen = new PlaylistResultsScreen(solo);
        assertEquals("Playlist results screen should be visible", true, resultsScreen.isVisible());
        assertEquals("Playlist results should not be empty", 20, resultsScreen.getResultsCount());
    }

    public void testClickingOnPlaylistTagPopulatesSearchField() {
        List<String> tags = playlistTagsScreen.getTags();

        playlistTagsScreen.clickOnTag(0);

        assertEquals(tags.get(0), playlistTagsScreen.actionBar().getSearchQuery());
    }

    public void testClickingOnPlaylistOpensPlaylistActivity() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.clickOnTag(0);

        PlaylistDetailsScreen detailsScreen = resultsScreen.clickOnPlaylist(0);
        assertEquals("Playlist details screen should be shown", true, detailsScreen.isVisible());
    }

    public void testSearchingWithHashtagQueryShowsPlaylistDiscoveryResults() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.actionBar().doTagSearch("#deep house");
        assertEquals("Playlist results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testSearchingHashtagFromSuggestionShortcutShowsPlaylistDiscoveryResults() {
        playlistTagsScreen.actionBar().setSearchQuery("#deep house");
        solo.clickInList(0);

        PlaylistResultsScreen resultsScreen = new PlaylistResultsScreen(solo);
        assertEquals("Playlist results screen should be visible", true, resultsScreen.isVisible());
    }

}
