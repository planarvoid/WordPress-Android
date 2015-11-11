package com.soundcloud.android.tests.search.intents;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.search.LegacySearchResultsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveVoiceCommandActionTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").putExtra(SearchManager.QUERY, "skrillex");
    }

    public void testVoiceActionSearchResolution() {
        LegacySearchResultsScreen resultsScreen = new LegacySearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen.isVisible());
        assertThat("Search query should be set", resultsScreen.actionBar().getLegacySearchQuery(), is("skrillex"));
    }
}
