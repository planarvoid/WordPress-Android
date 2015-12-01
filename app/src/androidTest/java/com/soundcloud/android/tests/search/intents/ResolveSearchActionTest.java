package com.soundcloud.android.tests.search.intents;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

import android.app.SearchManager;
import android.content.Intent;

public class ResolveSearchActionTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, "skrillex");
    }

    public void testSearchActionResolution() {
        final SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search query should be set", resultsScreen.getActionBarTitle(), is("skrillex"));
    }

    @Ignore
    public void testGoingBackFromActionResolutionShowsDiscoveryScreen() {
        final SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        final DiscoveryScreen discoveryScreen = resultsScreen.goBack();
        assertThat("Discovery screen should be visible", discoveryScreen, is(visible()));
    }
}
