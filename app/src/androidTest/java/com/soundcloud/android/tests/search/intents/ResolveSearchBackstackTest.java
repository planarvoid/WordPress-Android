package com.soundcloud.android.tests.search.intents;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.screens.search.LegacySearchResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveSearchBackstackTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, "skrillex");
    }

    public void testClearingQueryShowsPlaylistTagsScreen() {
        LegacySearchResultsScreen resultsScreen = new LegacySearchResultsScreen(solo);
        PlaylistTagsScreen tagsScreen = resultsScreen.actionBar().legacyDismissSearch();
        assertThat("Search tags screen should be visible", tagsScreen.isVisible());
    }

    public void testGoingBackShowsPlaylistTagsScreen() {
        LegacySearchResultsScreen resultsScreen = new LegacySearchResultsScreen(solo);
        resultsScreen.pressBack();
        PlaylistTagsScreen playlistTagsScreen = new PlaylistTagsScreen(solo);
        assertThat("Search tags screen should be visible", playlistTagsScreen.isVisible());
    }
}
