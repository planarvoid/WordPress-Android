package com.soundcloud.android.tests.search.intents;

import com.soundcloud.android.screens.search.SearchResultsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveSearchActionTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, "skrillex");
    }

    public void testSearchActionResolution() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
        assertEquals("Search query should be set", "skrillex", resultsScreen.actionBar().getSearchQuery());
    }

}
