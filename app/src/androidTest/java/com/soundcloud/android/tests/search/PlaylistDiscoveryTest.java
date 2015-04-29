package com.soundcloud.android.tests.search;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.Consts;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTest;

import java.util.List;

public class PlaylistDiscoveryTest extends ActivityTest<MainActivity> {

    private PlaylistTagsScreen playlistTagsScreen;

    public PlaylistDiscoveryTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        playlistTagsScreen = new StreamScreen(solo).actionBar().clickSearchButton();
    }

    public void testTagsAreDisplayedWhenSearchScreenIsOpened() {
        assertTrue("Playlist tags should be visible", playlistTagsScreen.isDisplayingTags());
    }

    public void testTagDisplayedAsSuggestionAfterTagSearch() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.actionBar().doTagSearch("#deep house");
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().dismissSearch();

        assertTrue("Playlist tags screen should be visible", tagsScreen.isVisible());
        assertTrue("Searched tag should be in recents", tagsScreen.getRecentTags().contains("#deep house"));
    }

    public void testClickingOnPlaylistTagOpensPlaylistResultsScreenWithDefaultNumberOfResults() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.clickOnTag(0);

        assertTrue("Playlist results screen should be visible", resultsScreen.isVisible());
        assertThat("Playlist results should not be empty", resultsScreen.getResultsCount(), is(Consts.CARD_PAGE_SIZE));
    }

    public void testClickingOnPlaylistTagPopulatesSearchField() {
        List<ViewElement> tags = playlistTagsScreen.getTags();

        playlistTagsScreen.clickOnTag(0);

        assertEquals(new TextElement(tags.get(0)).getText(), playlistTagsScreen.actionBar().getSearchQuery());
    }

    public void testClickingOnPlaylistOpensDoesPlaylistActivity() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.clickOnTag(0);

        PlaylistDetailsScreen detailsScreen = resultsScreen.clickOnPlaylist(0);
        assertTrue("Playlist details screen should be shown", detailsScreen.isVisible());
    }

    public void testSearchingWithHashtagQueryShowsPlaylistDiscoveryResults() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.actionBar().doTagSearch("#deep house");
        assertTrue("Playlist results screen should be visible", resultsScreen.isVisible());
    }

    public void testSearchingHashtagFromSuggestionShortcutShowsPlaylistDiscoveryResults() {
        playlistTagsScreen.actionBar().setSearchQuery("#deep house");
        //TODO: That should actually be handled buy SearchSuggestionsElement class
        solo.clickOnText("Search for '#deep house'");

        PlaylistResultsScreen resultsScreen = new PlaylistResultsScreen(solo);
        assertTrue("Playlist results screen should be visible", resultsScreen.isVisible());
    }

    public void testSearchingEmptyHashtagDoesNotPerformSearch() {
        playlistTagsScreen.actionBar().doTagSearch("#");
        assertTrue("Screen should not change", playlistTagsScreen.isVisible());
    }

    public void testGoingBackFromPlayResultsReturnsToTagPage() {
        PlaylistResultsScreen resultsScreen = playlistTagsScreen.clickOnTag(0);

        playlistTagsScreen = resultsScreen.pressBack();
        assertTrue("Main screen should be visible", playlistTagsScreen.isVisible());
        assertThat("Search query should be empty", playlistTagsScreen.actionBar().getSearchQuery(), is(""));
    }
}
