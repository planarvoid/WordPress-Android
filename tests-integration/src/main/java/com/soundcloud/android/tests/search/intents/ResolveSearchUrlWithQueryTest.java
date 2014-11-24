package com.soundcloud.android.tests.search.intents;

import com.soundcloud.android.framework.screens.search.SearchResultsScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrlWithQueryTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://soundcloud.com/search/sounds?q=skrillex"));
    }

    public void testSearchQueryUrlResolution() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
        assertEquals("Search query should be set", "skrillex", resultsScreen.actionBar().getSearchQuery());
    }

}
