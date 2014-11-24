package com.soundcloud.android.tests.search.intents;

import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveSearchBackstackTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, "skrillex");
    }

    public void testClearingQueryShowsPlaylistTagsScreen() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().dismissSearch();
        assertEquals("Search tags screen should be visible", true, tagsScreen.isVisible());
    }

    public void testGoingBackShowsPlaylistTagsScreen() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        resultsScreen.pressBack();
        PlaylistTagsScreen playlistTagsScreen = new PlaylistTagsScreen(solo);
        assertEquals("Search tags screen should be visible", true, playlistTagsScreen.isVisible());
    }

}
