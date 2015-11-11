package com.soundcloud.android.tests.search.intents;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.search.LegacySearchResultsScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchContentUriWithQueryTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/search/skrillex"));
    }

    public void testSearchContentUriResolution() {
        LegacySearchResultsScreen resultsScreen = new LegacySearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen.isVisible());
        assertThat("Search query should be set", resultsScreen.actionBar().getLegacySearchQuery(), is("skrillex"));
    }
}
