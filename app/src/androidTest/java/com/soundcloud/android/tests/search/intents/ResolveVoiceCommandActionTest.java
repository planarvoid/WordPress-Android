package com.soundcloud.android.tests.search.intents;

import static android.app.SearchManager.QUERY;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import org.junit.Test;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveVoiceCommandActionTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").putExtra(QUERY, "skrillex");
    }

    @Test
    public void testVoiceActionSearchResolution() throws Exception {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search query should be set", resultsScreen.actionBar().getSearchQuery(), is("skrillex"));
    }
}
