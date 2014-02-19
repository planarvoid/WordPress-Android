package com.soundcloud.android.search;

import com.soundcloud.android.screens.search.SearchResultsScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrlWithQuery extends SearchIntentsBase {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://soundcloud.com/search/sounds?q=skrillex"));
    }

    public void testSearchQueryUrlResolution() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
    }

}
