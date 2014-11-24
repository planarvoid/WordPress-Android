package com.soundcloud.android.tests.search.intents;

import com.soundcloud.android.framework.screens.search.SearchResultsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveVoiceCommandActionTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").putExtra(SearchManager.QUERY, "skrillex");
    }

    public void testVoiceActionSearchResolution() {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertEquals("Search results screen should be visible", true, resultsScreen.isVisible());
        assertEquals("Search query should be set", "skrillex", resultsScreen.actionBar().getSearchQuery());
    }

}
