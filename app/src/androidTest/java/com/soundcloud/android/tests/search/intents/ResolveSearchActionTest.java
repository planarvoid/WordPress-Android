package com.soundcloud.android.tests.search.intents;

import static android.app.SearchManager.QUERY;
import static android.content.Intent.ACTION_SEARCH;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import org.junit.Test;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveSearchActionTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(ACTION_SEARCH).putExtra(QUERY, "skrillex");
    }

    @Test
    public void testSearchActionResolution() throws Exception {
        final SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search query should be set", resultsScreen.getActionBarTitle(), is("skrillex"));
    }
}
