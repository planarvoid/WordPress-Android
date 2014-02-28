package com.soundcloud.android.search.intents;

import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveSearchBackstack extends SearchIntentsBase {

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
        PlaylistTagsScreen tagsScreen = resultsScreen.pressBack();
        assertEquals("Search tags screen should be visible", true, tagsScreen.isVisible());
    }

}
