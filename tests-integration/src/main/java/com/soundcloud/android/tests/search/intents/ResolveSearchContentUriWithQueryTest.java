package com.soundcloud.android.tests.search.intents;

import com.soundcloud.android.framework.screens.search.SearchResultsScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchContentUriWithQueryTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/search/skrillex"));
    }

    public void testSearchContentUriResolution() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
        assertEquals("Search query should be set", "skrillex", resultsScreen.actionBar().getSearchQuery());
    }

}
