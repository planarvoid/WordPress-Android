package com.soundcloud.android.search;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.viewelements.TextElement;
import com.soundcloud.android.tests.viewelements.ViewElement;

import java.util.List;

public class PlaylistDiscovery extends ActivityTestCase<MainActivity> {

    private MainScreen mainScreen;
    private PlaylistTagsScreen playlistTagsScreen;

    public PlaylistDiscovery() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

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
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.clickOnTag(0);

        assertEquals("Playlist results screen should be visible", true, resultsScreen.isVisible());
        assertEquals("Playlist results should not be empty", 20, resultsScreen.getResultsCount());
    }

    public void testClickingOnPlaylistTagPopulatesSearchField() {
        List<ViewElement> tags = playlistTagsScreen.getTags();

        playlistTagsScreen.clickOnTag(0);

        assertEquals(new TextElement(tags.get(0)).getText(), playlistTagsScreen.actionBar().getSearchQuery());
    }

    public void testClickingOnPlaylistOpensDoesPlaylistActivity() {
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
        //TODO: That should actually be handled buy SearchSuggestionsElement class
        solo.clickOnText("Search for '#deep house'");
        PlaylistResultsScreen resultsScreen = new PlaylistResultsScreen(solo);
        assertEquals("Playlist results screen should be visible", true, resultsScreen.isVisible());
    }

    public void testSearchingEmptyHashtagDoesNotPerformSearch() {
        playlistTagsScreen.actionBar().doTagSearch("#");
        assertEquals("Screen should not change", true, playlistTagsScreen.isVisible());
    }

    public void testGoingBackFromPlayResultsReturnsToTagPage() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.clickOnTag(0);

        playlistTagsScreen = resultsScreen.pressBack();
        assertEquals("Main screen should be visible", true, playlistTagsScreen.isVisible());
        assertEquals("Search query should be empty", "", playlistTagsScreen.actionBar().getSearchQuery());
    }
}
